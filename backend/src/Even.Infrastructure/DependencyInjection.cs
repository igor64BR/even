using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Even.Application.Auth;
using Even.Application.Expenses;
using Even.Application.Groups;
using Even.Application.Settlements;
using Even.Infrastructure.Auth;
using Even.Infrastructure.Expenses;
using Even.Infrastructure.Groups;
using Even.Infrastructure.Notifications;
using Even.Infrastructure.Persistence;
using Even.Infrastructure.Settlements;

namespace Even.Infrastructure;

/// <summary>
/// DI registration entry point for the Infrastructure layer. Called from the composition root
/// (<c>Even.Api/Program.cs</c>) — keeps Program.cs lean instead of spreading each layer's
/// <c>services.AddX</c> directly into it.
/// </summary>
public static class DependencyInjection
{
    private const string ConnectionStringName = "Default";

    /// <summary>
    /// Registers <see cref="AppDbContext"/> (Npgsql) and the rest of Infrastructure's services.
    /// The connection string comes from <paramref name="configuration"/> (appsettings/environment
    /// variable) — never hardcoded.
    /// </summary>
    public static IServiceCollection AddInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString(ConnectionStringName)
            ?? throw new InvalidOperationException(
                $"ConnectionStrings:{ConnectionStringName} not found in configuration.");

        services.AddDbContext<AppDbContext>(options => options.UseNpgsql(connectionString));

        // Infrastructure health check (`/health` reflects the DB's health).
        // If a SignalR Hub health check is added later, it goes here as a second
        // `.AddCheck(...)`/`.AddSignalRHub(...)` in the same chain, making the endpoint aggregate.
        services.AddHealthChecks()
            .AddNpgSql(connectionString, name: "postgres");

        services.AddAuthGoogle(configuration);
        services.AddJwtAuthentication();
        services.AddGroups();

        return services;
    }

    /// <summary>
    /// DI registration: Google ID token validation, issuing the app's own JWT, and persisting
    /// user/refresh token. Extracted from <see cref="AddInfrastructure"/>'s body only for
    /// readability — it's still part of the same composition root.
    /// </summary>
    private static IServiceCollection AddAuthGoogle(this IServiceCollection services, IConfiguration configuration)
    {
        // Bind + ValidateOnStart: if GoogleAuth:ClientId or any Jwt field is missing from
        // appsettings, the application fails to start instead of failing silently on the first
        // login (the options records' `required` properties guarantee this at bind time).
        services
            .AddOptions<GoogleAuthOptions>()
            .Bind(configuration.GetSection(GoogleAuthOptions.ConfigurationSection))
            .ValidateOnStart();

        services
            .AddOptions<JwtOptions>()
            .Bind(configuration.GetSection(JwtOptions.ConfigurationSection))
            .ValidateOnStart();

        services.AddSingleton(TimeProvider.System);

        services.AddScoped<IGoogleTokenValidator, GoogleTokenValidator>();
        services.AddScoped<IJwtIssuer, JwtIssuer>();
        services.AddScoped<IUserRepository, UserRepository>();
        services.AddScoped<IRefreshTokenRepository, RefreshTokenRepository>();

        return services;
    }

    /// <summary>
    /// Validates the JWT that <see cref="JwtIssuer"/> issues, so <c>[Authorize]</c> endpoints like
    /// <c>POST /groups/sync</c> can require it. Reuses <see cref="JwtOptions"/> (the same issuing
    /// config) to validate signature/issuer/audience, instead of duplicating those values.
    /// <c>MapInboundClaims = false</c> is needed to read the <c>sub</c> claim exactly as
    /// <see cref="JwtIssuer"/> issued it — without it the default handler remaps "sub" to the
    /// legacy URI of <see cref="System.Security.Claims.ClaimTypes.NameIdentifier"/>.
    ///
    /// <c>OnMessageReceived</c> is the only addition needed for the SignalR Hub
    /// (<c>EvenHub</c>, Even.Api) to work authenticated. A Hub client can't send an
    /// <c>Authorization</c> header on the WebSocket handshake — ASP.NET Core's own documentation
    /// recommends reading the token from the <c>access_token</c> query string in that case,
    /// restricted to the Hub's route (<see cref="NotificationHubRoute.Path"/>) so as not to open
    /// this alternate authentication path for regular REST endpoints, which still require the
    /// normal header.
    /// </summary>
    private static IServiceCollection AddJwtAuthentication(this IServiceCollection services)
    {
        services
            .AddOptions<JwtBearerOptions>(JwtBearerDefaults.AuthenticationScheme)
            .Configure<IOptions<JwtOptions>>((bearerOptions, jwtOptionsAccessor) =>
            {
                var jwtOptions = jwtOptionsAccessor.Value;

                bearerOptions.MapInboundClaims = false;
                bearerOptions.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuer = true,
                    ValidIssuer = jwtOptions.Issuer,
                    ValidateAudience = true,
                    ValidAudience = jwtOptions.Audience,
                    ValidateLifetime = true,
                    ValidateIssuerSigningKey = true,
                    IssuerSigningKey = new SymmetricSecurityKey(System.Text.Encoding.UTF8.GetBytes(jwtOptions.SigningKey)),
                    ClockSkew = TimeSpan.FromSeconds(30),
                };
                bearerOptions.Events = new JwtBearerEvents
                {
                    OnMessageReceived = context =>
                    {
                        var accessToken = context.Request.Query["access_token"];
                        var requestPath = context.HttpContext.Request.Path;

                        if (!string.IsNullOrEmpty(accessToken)
                            && requestPath.StartsWithSegments(NotificationHubRoute.Path))
                        {
                            context.Token = accessToken;
                        }

                        return Task.CompletedTask;
                    },
                };
            });

        services
            .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer();

        services.AddAuthorization();

        return services;
    }

    /// <summary>
    /// DI registration: syncing a local group to the cloud, adding a
    /// single new expense to an already-synced group, invite/join via code, and reading
    /// a group's current expenses/settlements to feed the simplification engine on demand.
    /// </summary>
    private static IServiceCollection AddGroups(this IServiceCollection services)
    {
        services.AddScoped<IGroupRepository, GroupRepository>();
        services.AddScoped<IExpenseRepository, ExpenseRepository>();
        services.AddScoped<IInviteCodeRepository, InviteCodeRepository>();
        services.AddScoped<ISettlementRepository, SettlementRepository>();

        return services;
    }
}

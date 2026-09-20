using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Rateio.Application.Auth;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Application.Quitacoes;
using Rateio.Infrastructure.Auth;
using Rateio.Infrastructure.Despesas;
using Rateio.Infrastructure.Grupos;
using Rateio.Infrastructure.Notificacoes;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Quitacoes;

namespace Rateio.Infrastructure;

/// <summary>
/// Ponto de registro de DI da camada de Infrastructure. Chamado a partir do composition root
/// (<c>Rateio.Api/Program.cs</c>) — mantém o Program.cs enxuto em vez de espalhar
/// <c>services.AddX</c> de cada camada diretamente nele (T3).
/// </summary>
public static class DependencyInjection
{
    private const string NomeConnectionString = "Default";

    /// <summary>
    /// Registra o <see cref="AppDbContext"/> (Npgsql) e demais serviços de Infrastructure.
    /// A connection string vem de <paramref name="configuration"/> (appsettings/variável de
    /// ambiente) — nunca hardcoded.
    /// </summary>
    public static IServiceCollection AddInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString(NomeConnectionString)
            ?? throw new InvalidOperationException(
                $"ConnectionStrings:{NomeConnectionString} não encontrada na configuração.");

        services.AddDbContext<AppDbContext>(options => options.UseNpgsql(connectionString));

        // T4.1: health check de infraestrutura para RNF08 (`/health` reflete a saúde do DB).
        // Quando o Hub SignalR (E8) existir, seu health check entra aqui como um segundo
        // `.AddCheck(...)`/`.AddSignalRHub(...)` na mesma chain, tornando o endpoint agregado.
        services.AddHealthChecks()
            .AddNpgSql(connectionString, name: "postgres");

        services.AddAuthGoogle(configuration);
        services.AddAutenticacaoJwt();
        services.AddGrupos();

        return services;
    }

    /// <summary>
    /// Registro de DI de T11: validação do ID token do Google, emissão de JWT próprio e
    /// persistência de usuário/refresh token. Extraído do corpo de <see cref="AddInfrastructure"/>
    /// só por legibilidade — continua fazendo parte do mesmo composition root.
    /// </summary>
    private static IServiceCollection AddAuthGoogle(this IServiceCollection services, IConfiguration configuration)
    {
        // Bind + ValidateOnStart: se faltar GoogleAuth:ClientId ou algum campo de Jwt em
        // appsettings, a aplicação falha ao subir em vez de falhar silenciosamente no primeiro
        // login (as propriedades `required` dos records de options garantem isso em tempo de bind).
        services
            .AddOptions<GoogleAuthOptions>()
            .Bind(configuration.GetSection(GoogleAuthOptions.SecaoConfiguracao))
            .ValidateOnStart();

        services
            .AddOptions<JwtOptions>()
            .Bind(configuration.GetSection(JwtOptions.SecaoConfiguracao))
            .ValidateOnStart();

        services.AddSingleton(TimeProvider.System);

        services.AddScoped<IGoogleTokenValidator, GoogleTokenValidator>();
        services.AddScoped<IJwtIssuer, JwtIssuer>();
        services.AddScoped<IUsuarioRepository, UsuarioRepository>();
        services.AddScoped<IRefreshTokenRepository, RefreshTokenRepository>();

        return services;
    }

    /// <summary>
    /// T18.1: <c>POST /groups/sync</c> exige JWT válido (<c>[Authorize]</c>) — até esta task
    /// ninguém validava o token que <see cref="JwtIssuer"/> emite, só o emitia (T11 nunca chegou a
    /// ter um endpoint protegido). Reaproveita <see cref="JwtOptions"/> (a mesma config de
    /// emissão) pra validar assinatura/issuer/audience, em vez de duplicar esses valores.
    /// <c>MapInboundClaims = false</c> é necessário pra ler o claim <c>sub</c> exatamente como
    /// <see cref="JwtIssuer"/> o emitiu — sem isso o handler padrão remapeia "sub" pro URI legado
    /// de <see cref="System.Security.Claims.ClaimTypes.NameIdentifier"/>.
    ///
    /// T38.1: <c>OnMessageReceived</c> é o único acréscimo pro Hub SignalR (<c>RateioHub</c>,
    /// Rateio.Api) funcionar autenticado. Um cliente de Hub não consegue mandar header
    /// <c>Authorization</c> no handshake de WebSocket — a própria documentação do ASP.NET Core
    /// recomenda ler o token da query string <c>access_token</c> nesse caso, restrito à rota do Hub
    /// (<see cref="RotaDoHubDeNotificacoes.Caminho"/>) pra não abrir esse caminho alternativo de
    /// autenticação pros endpoints REST comuns, que continuam exigindo o header normal.
    /// </summary>
    private static IServiceCollection AddAutenticacaoJwt(this IServiceCollection services)
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
                        var caminhoDaRequisicao = context.HttpContext.Request.Path;

                        if (!string.IsNullOrEmpty(accessToken)
                            && caminhoDaRequisicao.StartsWithSegments(RotaDoHubDeNotificacoes.Caminho))
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
    /// Registro de DI de T18/T23/T21/T32: sincronização de grupo local pra nuvem (RF09), adicionar
    /// despesa avulsa a um grupo já sincronizado (T23), convite/entrada por código (T21) e ler
    /// despesas/quitações vigentes de um grupo pra alimentar o motor de simplificação sob demanda
    /// (T32).
    /// </summary>
    private static IServiceCollection AddGrupos(this IServiceCollection services)
    {
        services.AddScoped<IGrupoRepository, GrupoRepository>();
        services.AddScoped<IDespesaRepository, DespesaRepository>();
        services.AddScoped<ICodigoConviteRepository, CodigoConviteRepository>();
        services.AddScoped<IQuitacaoRepository, QuitacaoRepository>();

        return services;
    }
}

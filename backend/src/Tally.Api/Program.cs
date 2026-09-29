using Tally.Api.Hubs;
using Tally.Api.Middleware;
using Tally.Application;
using Tally.Application.Notifications;
using Tally.Infrastructure;
using Tally.Infrastructure.Notifications;
using Serilog;
using Serilog.Formatting.Compact;

var builder = WebApplication.CreateBuilder(args);

// Serilog replaces ASP.NET Core's default log providers (RNF11: structured JSON logging per
// request). CompactJsonFormatter emits one JSON object per line, ready for a log aggregator.
// `ReadFrom.Configuration` allows adjusting levels via appsettings without a redeploy;
// `Enrich.FromLogContext()` is what lets CorrelationIdMiddleware attach the correlation id to every
// log event emitted during the request.
builder.Host.UseSerilog((context, services, loggerConfiguration) => loggerConfiguration
    .ReadFrom.Configuration(context.Configuration)
    .ReadFrom.Services(services)
    .Enrich.FromLogContext()
    .WriteTo.Console(new CompactJsonFormatter()));

// Add services to the container.

builder.Services.AddControllers();
builder.Services.AddApplication();
builder.Services.AddInfrastructure(builder.Configuration);
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// T38.1: TallyHub (RF35/RF36) — the real implementation of IGroupEventNotifier (Application) lives
// here, in Tally.Api, because that's where IHubContext<TallyHub> exists; registered in the
// composition root instead of in Tally.Infrastructure.DependencyInjection.AddInfrastructure
// because Infrastructure doesn't reference Tally.Api (TallyHub lives in the outermost layer).
builder.Services.AddSignalR();
builder.Services.AddScoped<IGroupEventNotifier, SignalRGroupEventNotifier>();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// Correlation id first: it needs to be active before Serilog's request/response log (and any other
// middleware) so that log already carries the id.
app.UseCorrelationId();
app.UseSerilogRequestLogging();

app.UseHttpsRedirection();

// T18: the project's first protected endpoint (`POST /groups/sync`) — UseAuthentication needs to
// run before UseAuthorization so HttpContext.User is populated from the JWT before [Authorize]
// decides whether to let the request through.
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

// T38.1: same route that Tally.Infrastructure.DependencyInjection.AddJwtAuthentication uses to
// know a request is a Hub handshake (and to read the JWT from the query string instead of the
// Authorization header).
app.MapHub<TallyHub>(NotificationHubRoute.Path);

// RNF08: 200 when dependencies (today: the DB) are healthy, 503 otherwise. Checks registered in
// Tally.Infrastructure.DependencyInjection.AddInfrastructure.
app.MapHealthChecks("/health");

app.Run();

using Rateio.Api.Middleware;
using Rateio.Infrastructure;
using Serilog;
using Serilog.Formatting.Compact;

var builder = WebApplication.CreateBuilder(args);

// Serilog substitui os providers de log padrão do ASP.NET Core (RNF11: log estruturado em JSON
// por requisição). CompactJsonFormatter emite um objeto JSON por linha, pronto para um
// agregador de logs. `ReadFrom.Configuration` permite ajustar níveis via appsettings sem redeploy;
// `Enrich.FromLogContext()` é o que faz o CorrelationIdMiddleware conseguir anexar o correlation
// id a cada evento de log emitido durante a requisição.
builder.Host.UseSerilog((context, services, loggerConfiguration) => loggerConfiguration
    .ReadFrom.Configuration(context.Configuration)
    .ReadFrom.Services(services)
    .Enrich.FromLogContext()
    .WriteTo.Console(new CompactJsonFormatter()));

// Add services to the container.

builder.Services.AddControllers();
builder.Services.AddInfrastructure(builder.Configuration);
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// Correlation id primeiro: precisa estar ativo antes do log de request/response do Serilog (e de
// qualquer outro middleware) para que aquele log já carregue o id.
app.UseCorrelationId();
app.UseSerilogRequestLogging();

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

// RNF08: 200 quando as dependências (hoje: DB) estão saudáveis, 503 caso contrário.
// Checks registrados em Rateio.Infrastructure.DependencyInjection.AddInfrastructure.
app.MapHealthChecks("/health");

app.Run();

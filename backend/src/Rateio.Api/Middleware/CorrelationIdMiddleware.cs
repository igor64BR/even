using Serilog.Context;

namespace Rateio.Api.Middleware;

/// <summary>
/// Garante um identificador de correlação por requisição (RNF11 — observabilidade). Lê o header
/// <c>X-Correlation-Id</c> de entrada; se ausente, gera um novo. O valor é publicado no
/// <see cref="LogContext"/> do Serilog para que todo log estruturado emitido durante a requisição
/// (inclusive o log de request/response do <c>UseSerilogRequestLogging</c>) carregue o mesmo id, e
/// é devolvido no header de resposta para permitir correlacionar cliente, API e logs.
/// </summary>
/// <remarks>
/// Fica em <c>Rateio.Api</c>, não em <c>Rateio.Infrastructure</c>: é um middleware do pipeline
/// HTTP do ASP.NET Core (opera sobre <see cref="HttpContext"/>), não uma integração com um serviço
/// externo. O projeto Infrastructure hoje nem referencia o SDK Web (só <c>Microsoft.NET.Sdk</c>) —
/// movê-lo para lá exigiria puxar dependências de ASP.NET Core para uma camada que deveria
/// permanecer agnóstica de transporte HTTP, só para reexportar algo que só o composition root usa.
/// </remarks>
public sealed class CorrelationIdMiddleware
{
    public const string HeaderName = "X-Correlation-Id";
    private const string LogContextProperty = "CorrelationId";

    private readonly RequestDelegate _next;

    public CorrelationIdMiddleware(RequestDelegate next)
    {
        _next = next;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var correlationId = ResolveCorrelationId(context);

        context.Response.OnStarting(() =>
        {
            context.Response.Headers[HeaderName] = correlationId;
            return Task.CompletedTask;
        });

        using (LogContext.PushProperty(LogContextProperty, correlationId))
        {
            await _next(context);
        }
    }

    private static string ResolveCorrelationId(HttpContext context)
    {
        if (context.Request.Headers.TryGetValue(HeaderName, out var headerValue))
        {
            var value = headerValue.ToString();
            if (!string.IsNullOrWhiteSpace(value))
            {
                return value;
            }
        }

        return Guid.NewGuid().ToString();
    }
}

public static class CorrelationIdMiddlewareExtensions
{
    /// <summary>
    /// Registra o <see cref="CorrelationIdMiddleware"/> no pipeline. Deve vir antes de
    /// <c>UseSerilogRequestLogging</c> para que o log de conclusão da requisição já carregue o
    /// correlation id.
    /// </summary>
    public static IApplicationBuilder UseCorrelationId(this IApplicationBuilder app)
        => app.UseMiddleware<CorrelationIdMiddleware>();
}

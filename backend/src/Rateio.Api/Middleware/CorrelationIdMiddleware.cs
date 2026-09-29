using Serilog.Context;

namespace Rateio.Api.Middleware;

/// <summary>
/// Ensures a per-request correlation identifier (RNF11 — observability). Reads the incoming
/// <c>X-Correlation-Id</c> header; if absent, generates a new one. The value is published to
/// Serilog's <see cref="LogContext"/> so that every structured log emitted during the request
/// (including the request/response log from <c>UseSerilogRequestLogging</c>) carries the same id,
/// and it's returned in the response header to allow correlating client, API, and logs.
/// </summary>
/// <remarks>
/// Lives in <c>Rateio.Api</c>, not <c>Rateio.Infrastructure</c>: it's an ASP.NET Core HTTP pipeline
/// middleware (operates on <see cref="HttpContext"/>), not an integration with an external service.
/// The Infrastructure project today doesn't even reference the Web SDK (only
/// <c>Microsoft.NET.Sdk</c>) — moving it there would require pulling ASP.NET Core dependencies into
/// a layer that should stay agnostic of HTTP transport, just to re-export something only the
/// composition root uses.
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
    /// Registers <see cref="CorrelationIdMiddleware"/> in the pipeline. Must come before
    /// <c>UseSerilogRequestLogging</c> so the request-completion log already carries the
    /// correlation id.
    /// </summary>
    public static IApplicationBuilder UseCorrelationId(this IApplicationBuilder app)
        => app.UseMiddleware<CorrelationIdMiddleware>();
}

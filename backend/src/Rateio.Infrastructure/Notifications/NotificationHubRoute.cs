namespace Rateio.Infrastructure.Notifications;

/// <summary>
/// Path of the <c>RateioHub</c> (T38, <c>Rateio.Api.Hubs.RateioHub</c>) — a constant shared between
/// <see cref="DependencyInjection.AddAutenticacaoJwt"/> (which needs to know which requests are a
/// Hub handshake, to read the JWT from the <c>access_token</c> query string instead of the
/// <c>Authorization</c> header) and <c>Rateio.Api/Program.cs</c> (which maps the Hub at this route).
/// Lives in Infrastructure, not in Rateio.Api.Hubs, because it's Infrastructure that configures
/// <c>AddJwtBearer</c> and can't reference Rateio.Api (the dependency runs the other way) — and not
/// in Rateio.Application because it's an HTTP transport detail, not a domain/use-case one.
/// </summary>
public static class NotificationHubRoute
{
    public const string Path = "/hubs/rateio";
}

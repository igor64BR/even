namespace Tally.Infrastructure.Notifications;

/// <summary>
/// Path of the <c>Tally.Api.Hubs.TallyHub</c> — a constant shared between
/// <see cref="DependencyInjection.AddJwtAuthentication"/> (which needs to know which requests are a
/// Hub handshake, to read the JWT from the <c>access_token</c> query string instead of the
/// <c>Authorization</c> header) and <c>Tally.Api/Program.cs</c> (which maps the Hub at this route).
/// Lives in Infrastructure, not in Tally.Api.Hubs, because it's Infrastructure that configures
/// <c>AddJwtBearer</c> and can't reference Tally.Api (the dependency runs the other way) — and not
/// in Tally.Application because it's an HTTP transport detail, not a domain/use-case one.
/// </summary>
public static class NotificationHubRoute
{
    public const string Path = "/hubs/tally";
}

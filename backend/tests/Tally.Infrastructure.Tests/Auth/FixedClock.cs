namespace Tally.Infrastructure.Tests.Auth;

/// <summary>
/// Test <see cref="TimeProvider"/> that always returns the same instant, so the exact expiration
/// computed by <c>JwtIssuer</c> can be asserted without depending on the real clock (avoids flaky
/// tests from a few milliseconds' difference).
/// </summary>
internal sealed class FixedClock(DateTimeOffset now) : TimeProvider
{
    public override DateTimeOffset GetUtcNow() => now;
}

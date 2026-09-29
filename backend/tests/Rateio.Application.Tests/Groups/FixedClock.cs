namespace Rateio.Application.Tests.Groups;

/// <summary>
/// Test <see cref="TimeProvider"/> that always returns the same instant — same pattern as
/// <c>Rateio.Infrastructure.Tests.Auth.FixedClock</c> (T11), reproduced here because the two test
/// projects don't share code with each other.
/// </summary>
internal sealed class FixedClock(DateTimeOffset now) : TimeProvider
{
    public override DateTimeOffset GetUtcNow() => now;
}

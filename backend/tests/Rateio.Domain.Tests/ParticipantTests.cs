using Rateio.Domain;

namespace Rateio.Domain.Tests;

/// <summary>
/// Covers the guest vs authenticated distinction (RF06/RF07 — spec.md) and the validation of
/// <see cref="ParticipantName"/>/<see cref="GroupName"/> (cannot be empty/whitespace-only).
/// </summary>
public class ParticipantTests
{
    [Fact]
    public void Guest_CreatesParticipantWithoutAccount()
    {
        var id = ParticipantId.New();

        var participant = Participant.Guest(id, ParticipantName.Create("Ana"));

        Assert.True(participant.IsGuest);
        Assert.Equal(id, participant.Id);
        Assert.Equal("Ana", participant.Name.Value);
    }

    [Fact]
    public void Authenticated_CreatesParticipantLinkedToAccount()
    {
        var participant = Participant.Authenticated(ParticipantId.New(), ParticipantName.Create("Bruno"));

        Assert.False(participant.IsGuest);
    }

    [Fact]
    public void Equals_ComparesById_IgnoringOtherFields()
    {
        var id = ParticipantId.New();
        var asGuest = Participant.Guest(id, ParticipantName.Create("Ana"));
        var asAuthenticated = Participant.Authenticated(id, ParticipantName.Create("Authenticated Ana"));

        Assert.Equal(asGuest, asAuthenticated);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void ParticipantName_Create_WithEmptyValue_Fails(string invalidValue)
    {
        Assert.Throws<ArgumentException>(() => ParticipantName.Create(invalidValue));
    }

    [Fact]
    public void ParticipantName_Create_TrimsSurroundingWhitespace()
    {
        var name = ParticipantName.Create("  Ana  ");

        Assert.Equal("Ana", name.Value);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void GroupName_Create_WithEmptyValue_Fails(string invalidValue)
    {
        Assert.Throws<ArgumentException>(() => GroupName.Create(invalidValue));
    }
}

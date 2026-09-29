using Rateio.Domain;

namespace Rateio.Domain.Tests;

/// <summary>
/// Covers <see cref="Group"/>'s core invariant (T15.1 — at least 1 participant, always) and the
/// behavior of the named mutation methods (<see cref="Group.AddParticipant"/>,
/// <see cref="Group.RemoveParticipant"/>, <see cref="Group.MarkAsSynced"/>).
/// </summary>
public class GroupTests
{
    [Fact]
    public void Create_WithNoParticipants_Fails()
    {
        var exception = Assert.Throws<InvalidOperationException>(
            () => Group.Create(GroupName.Create("Trip"), GroupCategory.Trip, []));

        Assert.Contains("at least 1 participant", exception.Message);
    }

    [Fact]
    public void Create_WithOneParticipant_Works()
    {
        var you = GuestParticipant("You");

        var group = Group.Create(GroupName.Create("Trip"), GroupCategory.Trip, [you]);

        Assert.Single(group.Participants);
        Assert.Equal(you, group.Participants[0]);
        Assert.False(group.Synced);
    }

    [Fact]
    public void AddParticipant_WithNewId_IncludesInGroup()
    {
        var group = Group.Create(GroupName.Create("Trip"), GroupCategory.Trip, [GuestParticipant("You")]);
        var friend = GuestParticipant("Friend");

        group.AddParticipant(friend);

        Assert.Equal(2, group.Participants.Count);
        Assert.Contains(friend, group.Participants);
    }

    [Fact]
    public void AddParticipant_WithAlreadyExistingId_Fails()
    {
        var you = GuestParticipant("You");
        var group = Group.Create(GroupName.Create("Trip"), GroupCategory.Trip, [you]);
        var duplicate = Participant.Guest(you.Id, ParticipantName.Create("Other Name"));

        Assert.Throws<InvalidOperationException>(() => group.AddParticipant(duplicate));
    }

    [Fact]
    public void RemoveParticipant_WhenAtLeastOneRemains_Works()
    {
        var you = GuestParticipant("You");
        var friend = GuestParticipant("Friend");
        var group = Group.Create(GroupName.Create("Trip"), GroupCategory.Trip, [you, friend]);

        group.RemoveParticipant(friend.Id);

        Assert.Single(group.Participants);
        Assert.Equal(you, group.Participants[0]);
    }

    [Fact]
    public void RemoveParticipant_WhenOnlyOneIsLeft_Fails()
    {
        var you = GuestParticipant("You");
        var group = Group.Create(GroupName.Create("Trip"), GroupCategory.Trip, [you]);

        var exception = Assert.Throws<InvalidOperationException>(() => group.RemoveParticipant(you.Id));

        Assert.Contains("at least 1 participant", exception.Message);
        Assert.Single(group.Participants);
    }

    [Fact]
    public void RemoveParticipant_NotInGroup_Fails()
    {
        var you = GuestParticipant("You");
        var friend = GuestParticipant("Friend");
        var group = Group.Create(GroupName.Create("Trip"), GroupCategory.Trip, [you, friend]);

        Assert.Throws<InvalidOperationException>(() => group.RemoveParticipant(ParticipantId.New()));
    }

    [Fact]
    public void MarkAsSynced_ChangesFlagToTrue()
    {
        var group = Group.Create(GroupName.Create("Trip"), GroupCategory.Trip, [GuestParticipant("You")]);

        group.MarkAsSynced();

        Assert.True(group.Synced);
    }

    private static Participant GuestParticipant(string name) =>
        Participant.Guest(ParticipantId.New(), ParticipantName.Create(name));
}

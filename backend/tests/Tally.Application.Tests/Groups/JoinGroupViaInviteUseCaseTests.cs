using Moq;
using Tally.Application.Groups;
using Tally.Domain;

namespace Tally.Application.Tests.Groups;

/// <summary>
/// Covers this use case with mocked <see cref="IInviteCodeRepository"/>/<see cref="IGroupRepository"/>: no
/// test here touches EF/Postgres. Key behaviors: joining with a valid code adds the
/// participant, and an invalid/nonexistent code gets a 404 — the first two tests; the rest cover
/// expiration (same 404 exception, see <see cref="InviteCode"/>) and the defensive case of a group
/// that can no longer be found.
/// </summary>
public class JoinGroupViaInviteUseCaseTests
{
    private static readonly DateTimeOffset Now = new(2026, 1, 10, 12, 0, 0, TimeSpan.Zero);
    private const string UserName = "Carla";

    private readonly Mock<IInviteCodeRepository> _inviteCodeRepository = new();
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly FixedClock _clock = new(Now);

    private JoinGroupViaInviteUseCase CreateUseCase() =>
        new(_inviteCodeRepository.Object, _groupRepository.Object, _clock);

    [Fact]
    public async Task ExecuteAsync_ValidCode_AddsAuthenticatedParticipantAndReturnsTheGroup()
    {
        var groupId = Guid.NewGuid();
        var anaId = Guid.NewGuid();
        ConfigureValidCode("ABC12345", groupId);
        ConfigureGroupForEntry(groupId, ExistingAuthenticated(anaId, "Ana"));

        Participant? addedParticipant = null;
        var receivedGroupId = Guid.Empty;
        _groupRepository
            .Setup(r => r.AddParticipantAsync(It.IsAny<Guid>(), It.IsAny<Participant>(), It.IsAny<CancellationToken>()))
            .Callback<Guid, Participant, CancellationToken>((group, participant, _) =>
            {
                receivedGroupId = group;
                addedParticipant = participant;
            })
            .Returns(Task.CompletedTask);

        var useCase = CreateUseCase();

        var result = await useCase.ExecuteAsync("ABC12345", UserName);

        Assert.Equal(groupId, result);
        Assert.Equal(groupId, receivedGroupId);
        Assert.NotNull(addedParticipant);
        Assert.Equal(UserName, addedParticipant!.Name.Value);
        Assert.False(addedParticipant.IsGuest, "Whoever joins via invite is an authenticated participant, not a guest.");
        _groupRepository.Verify(
            r => r.AddParticipantAsync(groupId, It.IsAny<Participant>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentCode_ThrowsInvalidInviteCodeWithoutAddingParticipant()
    {
        _inviteCodeRepository
            .Setup(r => r.GetByCodeAsync("DOESNOTEXIST", It.IsAny<CancellationToken>()))
            .ReturnsAsync((InviteCode?)null);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<InvalidInviteCodeException>(
            () => useCase.ExecuteAsync("DOESNOTEXIST", UserName));

        Assert.Equal("DOESNOTEXIST", exception.Code);
        _groupRepository.Verify(
            r => r.AddParticipantAsync(It.IsAny<Guid>(), It.IsAny<Participant>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_ExpiredCode_ThrowsInvalidInviteCodeWithoutAddingParticipant()
    {
        var groupId = Guid.NewGuid();
        _inviteCodeRepository
            .Setup(r => r.GetByCodeAsync("EXPIRED", It.IsAny<CancellationToken>()))
            .ReturnsAsync(new InviteCode("EXPIRED", groupId, Now.AddDays(-8), Now.AddDays(-1)));

        var useCase = CreateUseCase();

        await Assert.ThrowsAsync<InvalidInviteCodeException>(
            () => useCase.ExecuteAsync("EXPIRED", UserName));

        _groupRepository.Verify(
            r => r.AddParticipantAsync(It.IsAny<Guid>(), It.IsAny<Participant>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_GroupFromTheInviteNoLongerExists_ThrowsGroupNotFound()
    {
        var groupId = Guid.NewGuid();
        ConfigureValidCode("ABC12345", groupId);
        _groupRepository
            .Setup(r => r.GetForEntryAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GroupEntryInfo?)null);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<GroupNotFoundException>(
            () => useCase.ExecuteAsync("ABC12345", UserName));

        Assert.Equal(groupId, exception.GroupId);
    }

    private void ConfigureValidCode(string code, Guid groupId) =>
        _inviteCodeRepository
            .Setup(r => r.GetByCodeAsync(code, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new InviteCode(code, groupId, Now.AddDays(-1), Now.AddDays(6)));

    private void ConfigureGroupForEntry(Guid groupId, params Participant[] existingParticipants) =>
        _groupRepository
            .Setup(r => r.GetForEntryAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupEntryInfo(
                GroupName.Create("Trip"),
                GroupCategory.Trip,
                existingParticipants));

    private static Participant ExistingAuthenticated(Guid id, string name) =>
        Participant.Authenticated(new ParticipantId(id), ParticipantName.Create(name));
}

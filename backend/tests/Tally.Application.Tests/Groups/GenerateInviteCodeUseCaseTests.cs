using Moq;
using Tally.Application.Groups;

namespace Tally.Application.Tests.Groups;

/// <summary>
/// Covers T21.1 with mocked <see cref="IGroupRepository"/>/<see cref="IInviteCodeRepository"/>
/// (same pattern as <c>CreateExpenseUseCaseTests</c>, T23): no test here touches EF/Postgres. The
/// task's deliverable — "generating a code only works for the owner (another user gets a 403)" —
/// is the focus of the first two tests; the rest cover a nonexistent group (404) and the expiration
/// policy documented in <see cref="InviteCode"/>.
/// </summary>
public class GenerateInviteCodeUseCaseTests
{
    private static readonly DateTimeOffset Now = new(2026, 1, 10, 12, 0, 0, TimeSpan.Zero);

    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<IInviteCodeRepository> _inviteCodeRepository = new();
    private readonly FixedClock _clock = new(Now);

    private GenerateInviteCodeUseCase CreateUseCase() =>
        new(_groupRepository.Object, _inviteCodeRepository.Object, _clock);

    [Fact]
    public async Task ExecuteAsync_GroupOwner_GeneratesAndPersistsACodeValidForSevenDays()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        InviteCode? saved = null;
        _inviteCodeRepository
            .Setup(r => r.SaveAsync(It.IsAny<InviteCode>(), It.IsAny<CancellationToken>()))
            .Callback<InviteCode, CancellationToken>((code, _) => saved = code)
            .Returns(Task.CompletedTask);

        var useCase = CreateUseCase();

        var result = await useCase.ExecuteAsync(ownerUserId, groupId);

        Assert.NotNull(saved);
        Assert.Equal(result, saved);
        Assert.Equal(groupId, saved!.GroupId);
        Assert.Equal(8, saved.Value.Length);
        Assert.Equal(Now, saved.CreatedAt);
        Assert.Equal(Now.AddDays(7), saved.ExpiresAt);
        _inviteCodeRepository.Verify(
            r => r.SaveAsync(It.IsAny<InviteCode>(), It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task ExecuteAsync_UserWhoIsNotTheOwner_ThrowsAccessDeniedWithoutGeneratingCode()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var otherUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<AccessDeniedException>(
            () => useCase.ExecuteAsync(otherUserId, groupId));

        Assert.Equal(otherUserId, exception.UserId);
        Assert.Equal(groupId, exception.GroupId);
        _inviteCodeRepository.Verify(
            r => r.SaveAsync(It.IsAny<InviteCode>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentGroup_ThrowsGroupNotFoundWithoutGeneratingCode()
    {
        var groupId = Guid.NewGuid();
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GroupAccess?)null);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<GroupNotFoundException>(
            () => useCase.ExecuteAsync(Guid.NewGuid(), groupId));

        Assert.Equal(groupId, exception.GroupId);
        _inviteCodeRepository.Verify(
            r => r.SaveAsync(It.IsAny<InviteCode>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    private void ConfigureAccess(Guid groupId, Guid ownerUserId) =>
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));
}

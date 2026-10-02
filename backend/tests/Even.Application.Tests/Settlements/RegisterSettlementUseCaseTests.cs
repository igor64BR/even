using Moq;
using Even.Application.Groups;
using Even.Application.Notifications;
using Even.Application.Settlements;
using Even.Domain;

namespace Even.Application.Tests.Settlements;

/// <summary>
/// Covers this use case with mocked <see cref="IGroupRepository"/>/<see cref="ISettlementRepository"/>
/// (same pattern as <c>CreateExpenseUseCaseTests</c>): no test here touches EF/Postgres. The
/// key behaviors — a valid settlement persists in the right group, a nonexistent
/// group becomes <see cref="GroupNotFoundException"/> (the controller maps it to 404), a user
/// without access becomes <see cref="AccessDeniedException"/> (controller maps it to 403), an
/// invalid payload never persists — each has its own test. Reflecting in the next call to
/// GET /groups/{id}/settlement is covered separately by
/// <c>RegisterSettlementReflectsInSettlementTests</c>, which exercises the two use cases chained
/// against the same in-memory repository. This also covers
/// <see cref="IGroupEventNotifier"/>: mocked here (no real Hub), with its own test confirming the
/// right event fires after persisting.
/// </summary>
public class RegisterSettlementUseCaseTests
{
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<ISettlementRepository> _settlementRepository = new();
    private readonly Mock<IGroupEventNotifier> _groupEventNotifier = new();

    private RegisterSettlementUseCase CreateUseCase() =>
        new(_groupRepository.Object, _settlementRepository.Object, _groupEventNotifier.Object);

    [Fact]
    public async Task ExecuteAsync_ValidSettlementAndUserWithAccess_PersistsInTheCorrectGroup()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        Settlement? captured = null;
        var receivedGroupId = Guid.Empty;
        _settlementRepository
            .Setup(r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<Settlement>(), It.IsAny<CancellationToken>()))
            .Callback<Guid, Settlement, CancellationToken>((group, settlement, _) =>
            {
                receivedGroupId = group;
                captured = settlement;
            })
            .Returns(Task.CompletedTask);

        var useCase = CreateUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var request = new RegisterSettlementRequest(anaId, brunoId, AmountCents: 400);

        var settlementId = await useCase.ExecuteAsync(ownerUserId, groupId, request);

        Assert.Equal(groupId, receivedGroupId);
        Assert.NotNull(captured);
        Assert.Equal(settlementId, captured!.Id);
        Assert.Equal(anaId, captured.PayerId.Value);
        Assert.Equal(brunoId, captured.PayeeId.Value);
        Assert.Equal(400, captured.Amount.Cents);

        _settlementRepository.Verify(
            r => r.AddAsync(groupId, It.IsAny<Settlement>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecuteAsync_ValidSettlementAndUserWithAccess_NotifiesDebtSettledEventAfterPersisting()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var callOrder = new List<string>();
        _settlementRepository
            .Setup(r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<Settlement>(), It.IsAny<CancellationToken>()))
            .Callback(() => callOrder.Add("persisted"))
            .Returns(Task.CompletedTask);

        IGroupEvent? notifiedEvent = null;
        _groupEventNotifier
            .Setup(n => n.NotifyAsync(It.IsAny<IGroupEvent>(), It.IsAny<CancellationToken>()))
            .Callback<IGroupEvent, CancellationToken>((groupEvent, _) =>
            {
                callOrder.Add("notified");
                notifiedEvent = groupEvent;
            })
            .Returns(Task.CompletedTask);

        var useCase = CreateUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var request = new RegisterSettlementRequest(anaId, brunoId, AmountCents: 400);

        var settlementId = await useCase.ExecuteAsync(ownerUserId, groupId, request);

        // The notification only makes sense after the settlement has already been persisted successfully.
        Assert.Equal(["persisted", "notified"], callOrder);

        var debtSettledEvent = Assert.IsType<DebtSettledEvent>(notifiedEvent);
        Assert.Equal(groupId, debtSettledEvent.GroupId);
        Assert.Equal(settlementId, debtSettledEvent.SettlementId);
        Assert.Equal(anaId, debtSettledEvent.FromParticipantId);
        Assert.Equal(brunoId, debtSettledEvent.ToParticipantId);
        Assert.Equal(400, debtSettledEvent.AmountCents);
        Assert.Equal(GroupEventType.DebtSettled, debtSettledEvent.Type);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentGroup_ThrowsGroupNotFoundWithoutPersisting()
    {
        var groupId = Guid.NewGuid();
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GroupAccess?)null);

        var useCase = CreateUseCase();
        var request = new RegisterSettlementRequest(Guid.NewGuid(), Guid.NewGuid(), AmountCents: 400);

        var exception = await Assert.ThrowsAsync<GroupNotFoundException>(
            () => useCase.ExecuteAsync(Guid.NewGuid(), groupId, request));

        Assert.Equal(groupId, exception.GroupId);
        _settlementRepository.Verify(
            r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<Settlement>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _groupEventNotifier.Verify(
            n => n.NotifyAsync(It.IsAny<IGroupEvent>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_UserWithoutAccessToTheGroup_ThrowsAccessDeniedWithoutPersisting()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var userWithoutAccessId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();
        var request = new RegisterSettlementRequest(Guid.NewGuid(), Guid.NewGuid(), AmountCents: 400);

        var exception = await Assert.ThrowsAsync<AccessDeniedException>(
            () => useCase.ExecuteAsync(userWithoutAccessId, groupId, request));

        Assert.Equal(userWithoutAccessId, exception.UserId);
        Assert.Equal(groupId, exception.GroupId);
        _settlementRepository.Verify(
            r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<Settlement>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _groupEventNotifier.Verify(
            n => n.NotifyAsync(It.IsAny<IGroupEvent>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_GroupOwner_HasAccessEvenWithoutBeingAListedParticipant()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);
        _settlementRepository
            .Setup(r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<Settlement>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var useCase = CreateUseCase();
        var request = new RegisterSettlementRequest(Guid.NewGuid(), Guid.NewGuid(), AmountCents: 400);

        await useCase.ExecuteAsync(ownerUserId, groupId, request);

        _settlementRepository.Verify(
            r => r.AddAsync(groupId, It.IsAny<Settlement>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Theory]
    [InlineData(0)]
    [InlineData(-100)]
    public async Task ExecuteAsync_NonPositiveAmount_FailsWithoutPersisting(long amountCents)
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();
        var request = new RegisterSettlementRequest(Guid.NewGuid(), Guid.NewGuid(), amountCents);

        await Assert.ThrowsAsync<ArgumentException>(
            () => useCase.ExecuteAsync(ownerUserId, groupId, request));

        _settlementRepository.Verify(
            r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<Settlement>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _groupEventNotifier.Verify(
            n => n.NotifyAsync(It.IsAny<IGroupEvent>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_PayerEqualToPayee_FailsWithoutPersisting()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var participantId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();
        var request = new RegisterSettlementRequest(participantId, participantId, AmountCents: 400);

        await Assert.ThrowsAsync<ArgumentException>(
            () => useCase.ExecuteAsync(ownerUserId, groupId, request));

        _settlementRepository.Verify(
            r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<Settlement>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _groupEventNotifier.Verify(
            n => n.NotifyAsync(It.IsAny<IGroupEvent>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    private void ConfigureAccess(Guid groupId, Guid ownerUserId) =>
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));
}

using Moq;
using Tally.Application.Expenses;
using Tally.Application.Groups;
using Tally.Application.Notifications;
using Tally.Domain;

namespace Tally.Application.Tests.Expenses;

/// <summary>
/// Covers this use case with mocked <see cref="IGroupRepository"/>/<see cref="IExpenseRepository"/> (same
/// pattern as <c>SyncGroupUseCaseTests</c>): no test here touches EF/Postgres. Key behaviors —
/// a valid expense persists, a nonexistent group becomes
/// <see cref="GroupNotFoundException"/> (the controller maps it to 404), a user without access
/// becomes <see cref="AccessDeniedException"/> (controller maps it to 403) — each has its own test.
/// This also covers <see cref="IGroupEventNotifier"/>: mocked here (no real Hub), with its
/// own test confirming the right event fires after persisting.
/// </summary>
public class CreateExpenseUseCaseTests
{
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<IExpenseRepository> _expenseRepository = new();
    private readonly Mock<IGroupEventNotifier> _groupEventNotifier = new();

    private CreateExpenseUseCase CreateUseCase() =>
        new(_groupRepository.Object, _expenseRepository.Object, _groupEventNotifier.Object);

    [Fact]
    public async Task ExecuteAsync_ValidExpenseAndUserWithAccess_PersistsInTheCorrectGroup()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        ExpenseToPersist? captured = null;
        var receivedGroupId = Guid.Empty;
        _expenseRepository
            .Setup(r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()))
            .Callback<Guid, ExpenseToPersist, CancellationToken>((group, expense, _) =>
            {
                receivedGroupId = group;
                captured = expense;
            })
            .Returns(Task.CompletedTask);

        var useCase = CreateUseCase();
        var request = ValidRequest(out var anaId, out var brunoId);

        var expenseId = await useCase.ExecuteAsync(ownerUserId, groupId, request);

        Assert.Equal(request.Id, expenseId);
        Assert.Equal(groupId, receivedGroupId);
        Assert.NotNull(captured);
        Assert.Equal(request.Description, captured!.Description);
        Assert.Equal(request.Date, captured.Date);
        Assert.Equal(anaId, captured.Expense.PayerId.Value);
        Assert.Equal(2, captured.Expense.Splits.Count);
        Assert.All(captured.Expense.Splits, p => Assert.IsType<ExpenseSplit.Equal>(p));
        // Confirms that both participants of the request were mapped, not just the payer.
        Assert.Contains(captured.Expense.Splits, p => p.ParticipantId.Value == brunoId);

        _expenseRepository.Verify(
            r => r.AddAsync(groupId, It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecuteAsync_ValidExpenseAndUserWithAccess_NotifiesExpenseCreatedEventAfterPersisting()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var callOrder = new List<string>();
        _expenseRepository
            .Setup(r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()))
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
        var request = ValidRequest(out var anaId, out _);

        var expenseId = await useCase.ExecuteAsync(ownerUserId, groupId, request);

        // The notification only makes sense after the expense has already been persisted successfully.
        Assert.Equal(["persisted", "notified"], callOrder);

        var expenseCreatedEvent = Assert.IsType<ExpenseCreatedEvent>(notifiedEvent);
        Assert.Equal(groupId, expenseCreatedEvent.GroupId);
        Assert.Equal(expenseId, expenseCreatedEvent.ExpenseId);
        Assert.Equal(request.Description, expenseCreatedEvent.Description);
        Assert.Equal(request.TotalAmountCents, expenseCreatedEvent.TotalAmountCents);
        Assert.Equal(anaId, expenseCreatedEvent.PayerId);
        Assert.Equal(GroupEventType.ExpenseCreated, expenseCreatedEvent.Type);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentGroup_ThrowsGroupNotFoundWithoutPersisting()
    {
        var groupId = Guid.NewGuid();
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GroupAccess?)null);

        var useCase = CreateUseCase();
        var request = ValidRequest(out _, out _);

        var exception = await Assert.ThrowsAsync<GroupNotFoundException>(
            () => useCase.ExecuteAsync(Guid.NewGuid(), groupId, request));

        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()),
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
        var request = ValidRequest(out _, out _);

        var exception = await Assert.ThrowsAsync<AccessDeniedException>(
            () => useCase.ExecuteAsync(userWithoutAccessId, groupId, request));

        Assert.Equal(userWithoutAccessId, exception.UserId);
        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()),
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
        _expenseRepository
            .Setup(r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var useCase = CreateUseCase();
        var request = ValidRequest(out _, out _);

        await useCase.ExecuteAsync(ownerUserId, groupId, request);

        _expenseRepository.Verify(
            r => r.AddAsync(groupId, It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecuteAsync_WeightedSplitWithoutWeight_FailsWithoutPersisting()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var invalidRequest = new SyncedExpenseRequest(
            Guid.NewGuid(),
            "Dinner",
            TotalAmountCents: 1000,
            PayerId: anaId,
            Date: new DateOnly(2026, 1, 10),
            SplitType: SplitTypeRequest.Weighted,
            Splits:
            [
                new SyncedExpenseSplitRequest(anaId, Weight: null, AmountCents: null),
                new SyncedExpenseSplitRequest(brunoId, Weight: null, AmountCents: null),
            ]);

        await Assert.ThrowsAsync<ArgumentException>(
            () => useCase.ExecuteAsync(ownerUserId, groupId, invalidRequest));

        _expenseRepository.Verify(
            r => r.AddAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _groupEventNotifier.Verify(
            n => n.NotifyAsync(It.IsAny<IGroupEvent>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    private void ConfigureAccess(Guid groupId, Guid ownerUserId) =>
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));

    private static SyncedExpenseRequest ValidRequest(out Guid anaId, out Guid brunoId)
    {
        anaId = Guid.NewGuid();
        brunoId = Guid.NewGuid();

        return new SyncedExpenseRequest(
            Guid.NewGuid(),
            "Dinner",
            TotalAmountCents: 1000,
            PayerId: anaId,
            Date: new DateOnly(2026, 1, 10),
            SplitType: SplitTypeRequest.Equal,
            Splits:
            [
                new SyncedExpenseSplitRequest(anaId, Weight: null, AmountCents: null),
                new SyncedExpenseSplitRequest(brunoId, Weight: null, AmountCents: null),
            ]);
    }
}

using Moq;
using Tally.Application.Expenses;
using Tally.Application.Groups;
using Tally.Application.Notifications;
using Tally.Application.Settlements;

namespace Tally.Application.Tests.Notifications;

/// <summary>
/// Covers this use case with mocked <see cref="IGroupRepository"/>/<see cref="IExpenseRepository"/>/
/// <see cref="ISettlementRepository"/> (same pattern as
/// <c>Simplification.GetDebtSimplificationUseCaseTests</c>) — no test here touches
/// EF/Postgres. Key behaviors: events after "since" come back correct and ordered (expenses and
/// settlements interleaved by <c>CreatedAt</c>), a group with no new events returns an empty list,
/// and group access is verified (nonexistent/inaccessible group throws before querying expenses or
/// settlements) — each has its own test.
/// </summary>
public class GetGroupEventsUseCaseTests
{
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<IExpenseRepository> _expenseRepository = new();
    private readonly Mock<ISettlementRepository> _settlementRepository = new();

    private GetGroupEventsUseCase CreateUseCase() =>
        new(_groupRepository.Object, _expenseRepository.Object, _settlementRepository.Object);

    [Fact]
    public async Task ExecuteAsync_ExpensesAndSettlementsAfterSince_ReturnsEventsInterleavedAndSortedByCreatedAt()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);
        var since = new DateTimeOffset(2026, 9, 19, 12, 0, 0, TimeSpan.Zero);

        var expensePayerId = Guid.NewGuid();
        var mostRecentExpense = new ExpenseOccurred(
            Guid.NewGuid(), "Dinner", 5000, expensePayerId, since.AddMinutes(30));
        var oldestExpense = new ExpenseOccurred(
            Guid.NewGuid(), "Groceries", 3000, expensePayerId, since.AddMinutes(10));

        var fromId = Guid.NewGuid();
        var toId = Guid.NewGuid();
        var middleSettlement = new SettlementOccurred(
            Guid.NewGuid(), fromId, toId, 1200, since.AddMinutes(20));

        _expenseRepository
            .Setup(r => r.GetOccurredSinceAsync(groupId, since, It.IsAny<CancellationToken>()))
            .ReturnsAsync([mostRecentExpense, oldestExpense]);
        _settlementRepository
            .Setup(r => r.GetOccurredSinceAsync(groupId, since, It.IsAny<CancellationToken>()))
            .ReturnsAsync([middleSettlement]);

        var useCase = CreateUseCase();

        var events = await useCase.ExecuteAsync(ownerUserId, groupId, since);

        Assert.Equal(3, events.Count);

        var first = Assert.IsType<ExpenseCreatedEvent>(events[0]);
        Assert.Equal(oldestExpense.Id, first.ExpenseId);
        Assert.Equal("Groceries", first.Description);
        Assert.Equal(3000, first.TotalAmountCents);
        Assert.Equal(expensePayerId, first.PayerId);
        Assert.Equal(groupId, first.GroupId);

        var second = Assert.IsType<DebtSettledEvent>(events[1]);
        Assert.Equal(middleSettlement.Id, second.SettlementId);
        Assert.Equal(fromId, second.FromParticipantId);
        Assert.Equal(toId, second.ToParticipantId);
        Assert.Equal(1200, second.AmountCents);
        Assert.Equal(groupId, second.GroupId);

        var third = Assert.IsType<ExpenseCreatedEvent>(events[2]);
        Assert.Equal(mostRecentExpense.Id, third.ExpenseId);
        Assert.Equal("Dinner", third.Description);
    }

    [Fact]
    public async Task ExecuteAsync_GroupWithNoNewEvents_ReturnsEmptyList()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);
        var since = DateTimeOffset.UtcNow;

        _expenseRepository
            .Setup(r => r.GetOccurredSinceAsync(groupId, since, It.IsAny<CancellationToken>()))
            .ReturnsAsync([]);
        _settlementRepository
            .Setup(r => r.GetOccurredSinceAsync(groupId, since, It.IsAny<CancellationToken>()))
            .ReturnsAsync([]);

        var useCase = CreateUseCase();

        var events = await useCase.ExecuteAsync(ownerUserId, groupId, since);

        Assert.Empty(events);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentGroup_ThrowsGroupNotFoundWithoutQueryingExpensesOrSettlements()
    {
        var groupId = Guid.NewGuid();
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GroupAccess?)null);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<GroupNotFoundException>(
            () => useCase.ExecuteAsync(Guid.NewGuid(), groupId, DateTimeOffset.UtcNow));

        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.GetOccurredSinceAsync(It.IsAny<Guid>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _settlementRepository.Verify(
            r => r.GetOccurredSinceAsync(It.IsAny<Guid>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_UserWithoutAccessToTheGroup_ThrowsAccessDeniedWithoutQueryingExpensesOrSettlements()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var userWithoutAccessId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<AccessDeniedException>(
            () => useCase.ExecuteAsync(userWithoutAccessId, groupId, DateTimeOffset.UtcNow));

        Assert.Equal(userWithoutAccessId, exception.UserId);
        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.GetOccurredSinceAsync(It.IsAny<Guid>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _settlementRepository.Verify(
            r => r.GetOccurredSinceAsync(It.IsAny<Guid>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    private void ConfigureAccess(Guid groupId, Guid ownerUserId) =>
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));
}

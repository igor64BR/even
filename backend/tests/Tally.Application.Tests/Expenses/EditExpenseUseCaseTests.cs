using Moq;
using Tally.Application.Expenses;
using Tally.Application.Groups;
using Tally.Domain;

namespace Tally.Application.Tests.Expenses;

/// <summary>
/// Covers this use case with mocked <see cref="IGroupRepository"/>/<see cref="IExpenseRepository"/> (same
/// pattern as <c>CreateExpenseUseCaseTests</c>): no test here touches EF/Postgres. Key behaviors:
/// a valid edit rebuilds the whole expense and persists it, access is verified (404
/// nonexistent group, 403 without access), and an expense that doesn't exist in that group also
/// becomes 404 — via <see cref="ExpenseNotFoundException"/>, mapped by the controller.
/// </summary>
public class EditExpenseUseCaseTests
{
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<IExpenseRepository> _expenseRepository = new();

    private EditExpenseUseCase CreateUseCase() => new(_groupRepository.Object, _expenseRepository.Object);

    [Fact]
    public async Task ExecuteAsync_ValidExpenseAndUserWithAccess_RebuildsAndPersistsWithTheRouteId()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        ExpenseToPersist? captured = null;
        var receivedGroupId = Guid.Empty;
        _expenseRepository
            .Setup(r => r.UpdateAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()))
            .Callback<Guid, ExpenseToPersist, CancellationToken>((group, expense, _) =>
            {
                receivedGroupId = group;
                captured = expense;
            })
            .ReturnsAsync(true);

        var useCase = CreateUseCase();
        // The request arrives with an Id different from the route's — the route's must prevail
        // (never trust identity coming from the body).
        var request = ValidRequest(Guid.NewGuid(), out var anaId, out var brunoId);

        await useCase.ExecuteAsync(ownerUserId, groupId, expenseId, request);

        Assert.Equal(groupId, receivedGroupId);
        Assert.NotNull(captured);
        Assert.Equal(expenseId, captured!.Expense.Id);
        Assert.Equal(request.Description, captured.Description);
        Assert.Equal(request.Date, captured.Date);
        Assert.Equal(anaId, captured.Expense.PayerId.Value);
        Assert.Equal(2, captured.Expense.Splits.Count);
        Assert.Contains(captured.Expense.Splits, p => p.ParticipantId.Value == brunoId);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentGroup_ThrowsGroupNotFoundWithoutPersisting()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GroupAccess?)null);

        var useCase = CreateUseCase();
        var request = ValidRequest(expenseId, out _, out _);

        var exception = await Assert.ThrowsAsync<GroupNotFoundException>(
            () => useCase.ExecuteAsync(Guid.NewGuid(), groupId, expenseId, request));

        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.UpdateAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_UserWithoutAccessToTheGroup_ThrowsAccessDeniedWithoutPersisting()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var userWithoutAccessId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();
        var request = ValidRequest(expenseId, out _, out _);

        var exception = await Assert.ThrowsAsync<AccessDeniedException>(
            () => useCase.ExecuteAsync(userWithoutAccessId, groupId, expenseId, request));

        Assert.Equal(userWithoutAccessId, exception.UserId);
        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.UpdateAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_ExpenseDoesNotExistInThatGroup_ThrowsExpenseNotFound()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);
        _expenseRepository
            .Setup(r => r.UpdateAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(false);

        var useCase = CreateUseCase();
        var request = ValidRequest(expenseId, out _, out _);

        var exception = await Assert.ThrowsAsync<ExpenseNotFoundException>(
            () => useCase.ExecuteAsync(ownerUserId, groupId, expenseId, request));

        Assert.Equal(groupId, exception.GroupId);
        Assert.Equal(expenseId, exception.ExpenseId);
    }

    [Fact]
    public async Task ExecuteAsync_FixedAmountSplitWithoutAmount_FailsWithoutPersisting()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var invalidRequest = new SyncedExpenseRequest(
            expenseId,
            "Dinner",
            TotalAmountCents: 1000,
            PayerId: anaId,
            Date: new DateOnly(2026, 1, 10),
            SplitType: SplitTypeRequest.FixedAmount,
            Splits:
            [
                new SyncedExpenseSplitRequest(anaId, Weight: null, AmountCents: null),
                new SyncedExpenseSplitRequest(brunoId, Weight: null, AmountCents: null),
            ]);

        await Assert.ThrowsAsync<ArgumentException>(
            () => useCase.ExecuteAsync(ownerUserId, groupId, expenseId, invalidRequest));

        _expenseRepository.Verify(
            r => r.UpdateAsync(It.IsAny<Guid>(), It.IsAny<ExpenseToPersist>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    private void ConfigureAccess(Guid groupId, Guid ownerUserId) =>
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));

    private static SyncedExpenseRequest ValidRequest(Guid requestId, out Guid anaId, out Guid brunoId)
    {
        anaId = Guid.NewGuid();
        brunoId = Guid.NewGuid();

        return new SyncedExpenseRequest(
            requestId,
            "Edited dinner",
            TotalAmountCents: 2000,
            PayerId: anaId,
            Date: new DateOnly(2026, 2, 1),
            SplitType: SplitTypeRequest.Equal,
            Splits:
            [
                new SyncedExpenseSplitRequest(anaId, Weight: null, AmountCents: null),
                new SyncedExpenseSplitRequest(brunoId, Weight: null, AmountCents: null),
            ]);
    }
}

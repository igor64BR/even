using Moq;
using Even.Application.Expenses;
using Even.Application.Groups;
using Even.Domain;

namespace Even.Application.Tests.Groups;

/// <summary>
/// Covers this use case with mocked <see cref="IGroupRepository"/>/<see cref="IExpenseRepository"/> (same
/// pattern as <c>CreateExpenseUseCaseTests</c>): no test here touches EF/Postgres. Key behaviors:
/// an existing group with access returns name/category/participants/expenses together
/// (<see cref="FullGroup"/>), and access is verified (404 nonexistent group, 403 without access).
/// </summary>
public class GetGroupUseCaseTests
{
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<IExpenseRepository> _expenseRepository = new();

    private GetGroupUseCase CreateUseCase() => new(_groupRepository.Object, _expenseRepository.Object);

    [Fact]
    public async Task ExecuteAsync_ExistingGroupAndUserWithAccess_ReturnsParticipantsAndExpenses()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var anaId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var anaParticipant = Participant.Authenticated(new ParticipantId(anaId), ParticipantName.Create("Ana"));
        var groupEntryInfo = new GroupEntryInfo(GroupName.Create("Trip"), GroupCategory.Trip, [anaParticipant]);
        _groupRepository
            .Setup(r => r.GetForEntryAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(groupEntryInfo);

        var expense = new Expense(
            Guid.NewGuid(),
            Money.FromCents(1500),
            new ParticipantId(anaId),
            [new ExpenseSplit.Equal(new ParticipantId(anaId))]);
        var expenseToPersist = new ExpenseToPersist(expense, "Dinner", new DateOnly(2026, 1, 10));
        _expenseRepository
            .Setup(r => r.GetDetailedByGroupAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync([expenseToPersist]);

        var useCase = CreateUseCase();

        var result = await useCase.ExecuteAsync(ownerUserId, groupId);

        Assert.Equal("Trip", result.Group.Name.Value);
        Assert.Equal(GroupCategory.Trip, result.Group.Category);
        Assert.Single(result.Group.Participants);
        Assert.Equal(anaId, result.Group.Participants[0].Id.Value);
        Assert.Single(result.Expenses);
        Assert.Equal("Dinner", result.Expenses[0].Description);
        Assert.Equal(expense.Id, result.Expenses[0].Expense.Id);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentGroup_ThrowsGroupNotFound()
    {
        var groupId = Guid.NewGuid();
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GroupAccess?)null);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<GroupNotFoundException>(
            () => useCase.ExecuteAsync(Guid.NewGuid(), groupId));

        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.GetDetailedByGroupAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_UserWithoutAccessToTheGroup_ThrowsAccessDenied()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var userWithoutAccessId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<AccessDeniedException>(
            () => useCase.ExecuteAsync(userWithoutAccessId, groupId));

        Assert.Equal(userWithoutAccessId, exception.UserId);
        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.GetDetailedByGroupAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    private void ConfigureAccess(Guid groupId, Guid ownerUserId) =>
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));
}

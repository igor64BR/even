using Moq;
using Tally.Application.Expenses;
using Tally.Application.Groups;

namespace Tally.Application.Tests.Expenses;

/// <summary>
/// Covers T28.2 with mocked <see cref="IGroupRepository"/>/<see cref="IExpenseRepository"/> (same
/// pattern as <c>EditExpenseUseCaseTests</c>): no test here touches EF/Postgres. Task deliverable:
/// deletion respects RNF07 (404 nonexistent group, 403 without access) and an expense that doesn't
/// exist in that group becomes 404 via <see cref="ExpenseNotFoundException"/>.
/// </summary>
public class DeleteExpenseUseCaseTests
{
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<IExpenseRepository> _expenseRepository = new();

    private DeleteExpenseUseCase CreateUseCase() => new(_groupRepository.Object, _expenseRepository.Object);

    [Fact]
    public async Task ExecuteAsync_ExistingExpenseAndUserWithAccess_Removes()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);
        _expenseRepository
            .Setup(r => r.RemoveAsync(groupId, expenseId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(true);

        var useCase = CreateUseCase();

        await useCase.ExecuteAsync(ownerUserId, groupId, expenseId);

        _expenseRepository.Verify(r => r.RemoveAsync(groupId, expenseId, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentGroup_ThrowsGroupNotFoundWithoutRemoving()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GroupAccess?)null);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<GroupNotFoundException>(
            () => useCase.ExecuteAsync(Guid.NewGuid(), groupId, expenseId));

        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.RemoveAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_UserWithoutAccessToTheGroup_ThrowsAccessDeniedWithoutRemoving()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var userWithoutAccessId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<AccessDeniedException>(
            () => useCase.ExecuteAsync(userWithoutAccessId, groupId, expenseId));

        Assert.Equal(userWithoutAccessId, exception.UserId);
        Assert.Equal(groupId, exception.GroupId);
        _expenseRepository.Verify(
            r => r.RemoveAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_ExpenseDoesNotExistInThatGroup_ThrowsExpenseNotFound()
    {
        var groupId = Guid.NewGuid();
        var expenseId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);
        _expenseRepository
            .Setup(r => r.RemoveAsync(groupId, expenseId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(false);

        var useCase = CreateUseCase();

        var exception = await Assert.ThrowsAsync<ExpenseNotFoundException>(
            () => useCase.ExecuteAsync(ownerUserId, groupId, expenseId));

        Assert.Equal(groupId, exception.GroupId);
        Assert.Equal(expenseId, exception.ExpenseId);
    }

    private void ConfigureAccess(Guid groupId, Guid ownerUserId) =>
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));
}

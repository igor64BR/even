using System.Diagnostics;
using Moq;
using Rateio.Application.Expenses;
using Rateio.Application.Groups;
using Rateio.Application.Settlements;
using Rateio.Application.Simplification;
using Rateio.Domain;

namespace Rateio.Application.Tests.Simplification;

/// <summary>
/// Covers T32.1 with mocked <see cref="IGroupRepository"/>/<see cref="IExpenseRepository"/>/
/// <see cref="ISettlementRepository"/> (same pattern as <c>CreateExpenseUseCaseTests</c>, T23) — no
/// test here touches EF/Postgres. The engine (<see cref="DebtSimplificationEngine"/>, T31) is used
/// for real, not mocked: what this use case needs to prove is that it orchestrates correctly (loads
/// expenses+settlements of the right group, only after validating access, and passes them to the
/// engine without altering the result) — mocking the engine would hide exactly that.
/// </summary>
public class GetDebtSimplificationUseCaseTests
{
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<IExpenseRepository> _expenseRepository = new();
    private readonly Mock<ISettlementRepository> _settlementRepository = new();
    private readonly IDebtSimplificationEngine _engine = new DebtSimplificationEngine();

    private GetDebtSimplificationUseCase CreateUseCase() =>
        new(_groupRepository.Object, _expenseRepository.Object, _settlementRepository.Object, _engine);

    /// <summary>
    /// Fixture = case-01-simples from algorithm-spec.md, built from a real expense (not from
    /// already-computed balances, unlike how T31's test exercises this case) — B pays R$10.00 and A
    /// is the split's only participant, so ComputeBalances lands on A:-1000/B:+1000 even before
    /// ComputeSettlement is called.
    /// </summary>
    [Fact]
    public async Task ExecuteAsync_GroupWithExpenses_ReturnsCase01SimplesSimplification()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var a = new ParticipantId(Guid.NewGuid());
        var b = new ParticipantId(Guid.NewGuid());
        ConfigureAccess(groupId, ownerUserId);

        var expense = new Expense(
            Guid.NewGuid(),
            Money.FromCents(1000),
            PayerId: b,
            Splits: [new ExpenseSplit.FixedAmount(a, Money.FromCents(1000))]);
        _expenseRepository
            .Setup(r => r.GetByGroupAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { expense });
        _settlementRepository
            .Setup(r => r.GetByGroupAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(Array.Empty<Settlement>());

        var useCase = CreateUseCase();

        var transactions = await useCase.ExecuteAsync(ownerUserId, groupId);

        var expected = new[] { new Transaction(a, b, Money.FromCents(1000)) };
        Assert.Equal(expected, transactions);
    }

    [Fact]
    public async Task ExecuteAsync_GroupWithExpenseAndPartialSettlement_ReducesTheRemainingAmount()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var a = new ParticipantId(Guid.NewGuid());
        var b = new ParticipantId(Guid.NewGuid());
        ConfigureAccess(groupId, ownerUserId);

        // A owes 1000 to B; A already settled 400 of that debt -> only 600 left to simplify.
        var expense = new Expense(
            Guid.NewGuid(),
            Money.FromCents(1000),
            PayerId: b,
            Splits: [new ExpenseSplit.FixedAmount(a, Money.FromCents(1000))]);
        var settlement = new Settlement(Guid.NewGuid(), PayerId: a, PayeeId: b, Money.FromCents(400));
        _expenseRepository
            .Setup(r => r.GetByGroupAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { expense });
        _settlementRepository
            .Setup(r => r.GetByGroupAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { settlement });

        var useCase = CreateUseCase();

        var transactions = await useCase.ExecuteAsync(ownerUserId, groupId);

        var expected = new[] { new Transaction(a, b, Money.FromCents(600)) };
        Assert.Equal(expected, transactions);
    }

    [Fact]
    public async Task ExecuteAsync_NonexistentGroup_ThrowsGroupNotFoundWithoutReadingExpenses()
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
            r => r.GetByGroupAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
        _settlementRepository.Verify(
            r => r.GetByGroupAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_UserWithoutAccessToTheGroup_ThrowsAccessDeniedWithoutReadingExpenses()
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
            r => r.GetByGroupAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
        _settlementRepository.Verify(
            r => r.GetByGroupAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    /// <summary>
    /// RNF02 (spec.md): simplifying a group of up to 50 participants completes in under 200ms. The
    /// task assumed this was already covered by T31's tests, but
    /// <c>Rateio.Domain.Tests.DebtSimplificationEngineTests</c> has no timing test at all — just the
    /// five algorithm-spec.md cases and the split-type variations. It's covered here instead, at the
    /// full use-case level (more representative of the endpoint's real budget than timing the
    /// isolated engine alone).
    /// </summary>
    [Fact]
    public async Task ExecuteAsync_GroupWithFiftyParticipants_CompletesInUnder200Ms()
    {
        const int participantCount = 50;
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        ConfigureAccess(groupId, ownerUserId);

        var participants = Enumerable.Range(0, participantCount)
            .Select(_ => new ParticipantId(Guid.NewGuid()))
            .ToList();

        // Each participant pays a R$100.00 expense split equally among everyone — generates a
        // positive and negative balance spread across all 50, the costliest scenario for the greedy
        // algorithm (both queues full) instead of an already near-solved case.
        var expenses = participants
            .Select(payer => new Expense(
                Guid.NewGuid(),
                Money.FromCents(10000),
                PayerId: payer,
                Splits: participants.Select(p => (ExpenseSplit)new ExpenseSplit.Equal(p)).ToList()))
            .ToList();

        _expenseRepository
            .Setup(r => r.GetByGroupAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expenses);
        _settlementRepository
            .Setup(r => r.GetByGroupAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(Array.Empty<Settlement>());

        var useCase = CreateUseCase();

        var stopwatch = Stopwatch.StartNew();
        await useCase.ExecuteAsync(ownerUserId, groupId);
        stopwatch.Stop();

        Assert.True(
            stopwatch.ElapsedMilliseconds < 200,
            $"RNF02 violated: {stopwatch.ElapsedMilliseconds}ms for {participantCount} participants (limit: 200ms).");
    }

    private void ConfigureAccess(Guid groupId, Guid ownerUserId) =>
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));
}

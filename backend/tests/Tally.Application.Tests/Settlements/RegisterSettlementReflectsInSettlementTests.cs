using Moq;
using Tally.Application.Expenses;
using Tally.Application.Groups;
using Tally.Application.Notifications;
using Tally.Application.Settlements;
using Tally.Application.Simplification;
using Tally.Domain;

namespace Tally.Application.Tests.Settlements;

/// <summary>
/// T35.2: proves the deliverable criterion that the isolated tests of
/// <c>RegisterSettlementUseCaseTests</c> and <c>GetDebtSimplificationUseCaseTests</c> (T32) don't
/// cover on their own — that a settlement recorded by <see cref="RegisterSettlementUseCase"/>
/// (T35.1) is read back by <see cref="GetDebtSimplificationUseCase"/> (T32) on the next call,
/// without needing a cached balance. Uses a simple fake <see cref="ISettlementRepository"/>
/// (in-memory list) instead of a mock, because what this test needs is real state persisting
/// between the two use case calls — a mock of <c>AddAsync</c> wouldn't feed back into
/// <c>GetByGroupAsync</c>. Doesn't touch EF/Postgres: same "use case unit test" level as the other
/// files in this folder, just with a fake dependency instead of a mocked one.
/// </summary>
public class RegisterSettlementReflectsInSettlementTests
{
    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly Mock<IExpenseRepository> _expenseRepository = new();
    private readonly InMemorySettlementRepository _settlementRepository = new();
    private readonly IDebtSimplificationEngine _engine = new DebtSimplificationEngine();

    [Fact]
    public async Task RegisteredSettlement_ReducesTheAmountOnTheNextGetSimplificationCall()
    {
        var groupId = Guid.NewGuid();
        var ownerUserId = Guid.NewGuid();
        var a = new ParticipantId(Guid.NewGuid());
        var b = new ParticipantId(Guid.NewGuid());
        _groupRepository
            .Setup(r => r.GetAccessAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GroupAccess(groupId, ownerUserId));

        // A owes 1000 to B.
        var expense = new Expense(
            Guid.NewGuid(),
            Money.FromCents(1000),
            PayerId: b,
            Splits: [new ExpenseSplit.FixedAmount(a, Money.FromCents(1000))]);
        _expenseRepository
            .Setup(r => r.GetByGroupAsync(groupId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { expense });

        var getSimplification = new GetDebtSimplificationUseCase(
            _groupRepository.Object, _expenseRepository.Object, _settlementRepository, _engine);

        var beforeSettlement = await getSimplification.ExecuteAsync(ownerUserId, groupId);
        Assert.Equal([new Transaction(a, b, Money.FromCents(1000))], beforeSettlement);

        // T35.1: A pays 400 to B via POST /groups/{id}/settlements.
        var registerSettlement = new RegisterSettlementUseCase(
            _groupRepository.Object, _settlementRepository, Mock.Of<IGroupEventNotifier>());
        var request = new RegisterSettlementRequest(a.Value, b.Value, AmountCents: 400);
        await registerSettlement.ExecuteAsync(ownerUserId, groupId, request);

        var afterSettlement = await getSimplification.ExecuteAsync(ownerUserId, groupId);

        Assert.Equal([new Transaction(a, b, Money.FromCents(600))], afterSettlement);
    }

    /// <summary>
    /// Minimal fake of <see cref="ISettlementRepository"/> — just enough for this test to chain
    /// the two use cases against the same state, without pulling EF Core into an Application test.
    /// </summary>
    private sealed class InMemorySettlementRepository : ISettlementRepository
    {
        private readonly Dictionary<Guid, List<Settlement>> _byGroup = [];

        public Task<IReadOnlyList<Settlement>> GetByGroupAsync(
            Guid groupId, CancellationToken cancellationToken = default) =>
            Task.FromResult<IReadOnlyList<Settlement>>(
                _byGroup.TryGetValue(groupId, out var settlements) ? settlements : []);

        public Task AddAsync(Guid groupId, Settlement settlement, CancellationToken cancellationToken = default)
        {
            if (!_byGroup.TryGetValue(groupId, out var settlements))
            {
                settlements = [];
                _byGroup[groupId] = settlements;
            }

            settlements.Add(settlement);
            return Task.CompletedTask;
        }

        // T39.1: this fake only covers the settlement flow (GetByGroupAsync/AddAsync) that this
        // test exercises — the pull fallback has its own coverage in
        // Notifications.GetGroupEventsUseCaseTests, with a mocked repository (Moq), not this
        // in-memory fake.
        public Task<IReadOnlyList<SettlementOccurred>> GetOccurredSinceAsync(
            Guid groupId, DateTimeOffset since, CancellationToken cancellationToken = default) =>
            throw new NotSupportedException(
                $"{nameof(InMemorySettlementRepository)} does not implement {nameof(GetOccurredSinceAsync)} — out of scope for this test.");
    }
}

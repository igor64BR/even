using Moq;
using Tally.Application.Groups;
using Tally.Domain;

namespace Tally.Application.Tests.Groups;

/// <summary>
/// Covers this use case entirely with a mocked <see cref="IGroupRepository"/> (same pattern as
/// <c>AuthenticateWithGoogleUseCaseTests</c>): no test here touches EF/Postgres. The central part —
/// a group with expenses syncs correctly and the balance matches the engine's result — is verified
/// by capturing the <see cref="GroupToSync"/> the use case passed to the repository and feeding the
/// mapped expenses into the real <see cref="DebtSimplificationEngine"/>: if the DTO-to-domain
/// mapping (<see cref="SyncedGroupBuilder"/>) is right, the recomputed balance matches the value
/// expected by hand.
/// </summary>
public class SyncGroupUseCaseTests
{
    private static readonly Guid AuthenticatedUserId = Guid.NewGuid();

    private readonly Mock<IGroupRepository> _groupRepository = new();
    private readonly IDebtSimplificationEngine _engine = new DebtSimplificationEngine();

    private SyncGroupUseCase CreateUseCase() => new(_groupRepository.Object);

    [Fact]
    public async Task ExecuteAsync_ValidGroup_PersistsWithOwnerEqualToTheAuthenticatedUser()
    {
        GroupToSync? captured = null;
        _groupRepository
            .Setup(r => r.SyncAsync(It.IsAny<GroupToSync>(), It.IsAny<CancellationToken>()))
            .Callback<GroupToSync, CancellationToken>((group, _) => captured = group)
            .ReturnsAsync(Guid.NewGuid());

        var useCase = CreateUseCase();
        var request = RequestWithOneEqualSplitExpense(out _, out _);

        await useCase.ExecuteAsync(AuthenticatedUserId, request);

        Assert.NotNull(captured);
        Assert.Equal(AuthenticatedUserId, captured!.OwnerUserId);
        Assert.Equal(request.Name, captured.Group.Name.Value);
        Assert.Equal(request.Category, captured.Group.Category);
        Assert.True(captured.Group.Synced, "A synced group needs to become the source of truth.");
        Assert.Equal(2, captured.Group.Participants.Count);
        Assert.Single(captured.Expenses);
    }

    [Fact]
    public async Task ExecuteAsync_ReturnsTheGroupIdGivenBackByTheRepository()
    {
        var serverId = Guid.NewGuid();
        _groupRepository
            .Setup(r => r.SyncAsync(It.IsAny<GroupToSync>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(serverId);

        var useCase = CreateUseCase();
        var request = RequestWithOneEqualSplitExpense(out _, out _);

        var result = await useCase.ExecuteAsync(AuthenticatedUserId, request);

        Assert.Equal(serverId, result);
    }

    [Fact]
    public async Task ExecuteAsync_GroupWithEqualSplitExpense_BalanceRecomputedByTheEngineMatchesExpected()
    {
        GroupToSync? captured = null;
        _groupRepository
            .Setup(r => r.SyncAsync(It.IsAny<GroupToSync>(), It.IsAny<CancellationToken>()))
            .Callback<GroupToSync, CancellationToken>((group, _) => captured = group)
            .ReturnsAsync(Guid.NewGuid());

        var useCase = CreateUseCase();
        // Ana pays 1000 cents, split equally between Ana and Bruno: each ends up with half —
        // the same scenario as the engine's simplest case, just built from the payload
        // instead of a hand-built Expense.
        var request = RequestWithOneEqualSplitExpense(out var anaId, out var brunoId);

        await useCase.ExecuteAsync(AuthenticatedUserId, request);

        var mappedExpenses = captured!.Expenses.Select(d => d.Expense).ToList();
        var balances = _engine.ComputeBalances(mappedExpenses, settlements: []);

        Assert.Equal(Money.FromCents(500), balances[new ParticipantId(anaId)]);
        Assert.Equal(Money.FromCents(-500), balances[new ParticipantId(brunoId)]);

        var transactions = _engine.ComputeSettlement(balances);
        Assert.Equal(
            new[] { new Transaction(new ParticipantId(brunoId), new ParticipantId(anaId), Money.FromCents(500)) },
            transactions);
    }

    [Fact]
    public async Task ExecuteAsync_GroupWithWeightedAndFixedAmountSplits_MapsBothTypesCorrectly()
    {
        GroupToSync? captured = null;
        _groupRepository
            .Setup(r => r.SyncAsync(It.IsAny<GroupToSync>(), It.IsAny<CancellationToken>()))
            .Callback<GroupToSync, CancellationToken>((group, _) => captured = group)
            .ReturnsAsync(Guid.NewGuid());

        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var carlaId = Guid.NewGuid();
        var request = new SyncGroupRequest(
            "Trip",
            GroupCategory.Trip,
            Participants:
            [
                new SyncedParticipantRequest(anaId, "Ana", IsGuest: false),
                new SyncedParticipantRequest(brunoId, "Bruno", IsGuest: true),
                new SyncedParticipantRequest(carlaId, "Carla", IsGuest: true),
            ],
            Expenses:
            [
                new SyncedExpenseRequest(
                    Guid.NewGuid(),
                    "Lodging",
                    TotalAmountCents: 3000,
                    PayerId: anaId,
                    Date: new DateOnly(2026, 1, 10),
                    SplitType: SplitTypeRequest.Weighted,
                    Splits:
                    [
                        new SyncedExpenseSplitRequest(anaId, Weight: 2, AmountCents: null),
                        new SyncedExpenseSplitRequest(brunoId, Weight: 1, AmountCents: null),
                        new SyncedExpenseSplitRequest(carlaId, Weight: 1, AmountCents: null),
                    ]),
                new SyncedExpenseRequest(
                    Guid.NewGuid(),
                    "Toll",
                    TotalAmountCents: 900,
                    PayerId: brunoId,
                    Date: new DateOnly(2026, 1, 11),
                    SplitType: SplitTypeRequest.FixedAmount,
                    Splits:
                    [
                        new SyncedExpenseSplitRequest(anaId, Weight: null, AmountCents: 500),
                        new SyncedExpenseSplitRequest(brunoId, Weight: null, AmountCents: 400),
                    ]),
            ]);

        var useCase = CreateUseCase();

        await useCase.ExecuteAsync(AuthenticatedUserId, request);

        var mappedExpenses = captured!.Expenses.Select(d => d.Expense).ToList();
        var balances = _engine.ComputeBalances(mappedExpenses, settlements: []);

        // The sum of every balance is always zero, regardless of the split-type mix (engine
        // invariant).
        Assert.Equal(0, balances.Values.Sum(v => v.Cents));

        var lodging = mappedExpenses.Single(d => d.TotalAmount == Money.FromCents(3000));
        Assert.All(lodging.Splits, p => Assert.IsType<ExpenseSplit.Weighted>(p));

        var toll = mappedExpenses.Single(d => d.TotalAmount == Money.FromCents(900));
        Assert.All(toll.Splits, p => Assert.IsType<ExpenseSplit.FixedAmount>(p));
    }

    [Fact]
    public async Task ExecuteAsync_EmptyGroupName_FailsWithoutCallingTheRepository()
    {
        var useCase = CreateUseCase();
        var invalidRequest = RequestWithOneEqualSplitExpense(out _, out _) with { Name = "   " };

        await Assert.ThrowsAsync<ArgumentException>(() => useCase.ExecuteAsync(AuthenticatedUserId, invalidRequest));

        _groupRepository.Verify(
            r => r.SyncAsync(It.IsAny<GroupToSync>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_GroupWithNoParticipant_FailsByReusingGroupCreatesInvariant()
    {
        var useCase = CreateUseCase();
        var requestWithoutParticipants = new SyncGroupRequest(
            "Trip",
            GroupCategory.Trip,
            Participants: [],
            Expenses: []);

        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => useCase.ExecuteAsync(AuthenticatedUserId, requestWithoutParticipants));

        Assert.Contains("at least 1 participant", exception.Message);
    }

    [Fact]
    public async Task ExecuteAsync_WeightedSplitWithoutWeight_Fails()
    {
        var useCase = CreateUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var request = new SyncGroupRequest(
            "Trip",
            GroupCategory.Trip,
            Participants:
            [
                new SyncedParticipantRequest(anaId, "Ana", IsGuest: false),
                new SyncedParticipantRequest(brunoId, "Bruno", IsGuest: true),
            ],
            Expenses:
            [
                new SyncedExpenseRequest(
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
                    ]),
            ]);

        await Assert.ThrowsAsync<ArgumentException>(() => useCase.ExecuteAsync(AuthenticatedUserId, request));
    }

    private static SyncGroupRequest RequestWithOneEqualSplitExpense(out Guid anaId, out Guid brunoId)
    {
        anaId = Guid.NewGuid();
        brunoId = Guid.NewGuid();

        return new SyncGroupRequest(
            "Trip",
            GroupCategory.Trip,
            Participants:
            [
                new SyncedParticipantRequest(anaId, "Ana", IsGuest: false),
                new SyncedParticipantRequest(brunoId, "Bruno", IsGuest: true),
            ],
            Expenses:
            [
                new SyncedExpenseRequest(
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
                    ]),
            ]);
    }
}

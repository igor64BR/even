using Rateio.Application.Expenses;
using Rateio.Domain;

namespace Rateio.Application.Groups;

/// <summary>
/// DTO-to-domain mapping from T18.1 — the first time the project converts a JSON payload coming
/// from the app into the rich <c>Rateio.Domain</c> types (<see cref="Group"/>/<see cref="Participant"/>/
/// <see cref="Expense"/>). One named method per step, with no single method doing the whole
/// translation at once (Object Calisthenics) — and without bypassing domain validation: the
/// aggregate is always created by <see cref="Group.Create"/> (T15), never a loose constructor.
/// </summary>
internal static class SyncedGroupBuilder
{
    public static GroupToSync Build(Guid ownerUserId, SyncGroupRequest request)
    {
        var participants = BuildParticipants(request.Participants);
        var group = BuildGroup(request, participants);
        var expenses = BuildExpenses(request.Expenses);

        return new GroupToSync(group, ownerUserId, expenses);
    }

    private static Group BuildGroup(SyncGroupRequest request, IReadOnlyList<Participant> participants) =>
        Group.Create(GroupName.Create(request.Name), request.Category, participants);

    private static List<Participant> BuildParticipants(
        IReadOnlyList<SyncedParticipantRequest> participants) =>
        participants.Select(BuildParticipant).ToList();

    private static Participant BuildParticipant(SyncedParticipantRequest request)
    {
        var id = new ParticipantId(request.Id);
        var name = ParticipantName.Create(request.Name);

        return request.IsGuest
            ? Participant.Guest(id, name)
            : Participant.Authenticated(id, name);
    }

    private static List<ExpenseToPersist> BuildExpenses(
        IReadOnlyList<SyncedExpenseRequest> expenses) =>
        expenses.Select(BuildExpense).ToList();

    // T23 extracted building the Expense/ExpenseSplit itself into
    // Rateio.Application.Expenses.ExpenseMapper — reused here and by the new single-expense use
    // case, instead of duplicated.
    private static ExpenseToPersist BuildExpense(SyncedExpenseRequest request) =>
        new(ExpenseMapper.Build(request), request.Description, request.Date);
}

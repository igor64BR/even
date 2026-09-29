using Tally.Domain;

namespace Tally.Application.Settlements;

/// <summary>
/// DTO-to-domain mapping of a settlement (T35), mirroring
/// <c>Tally.Application.Expenses.ExpenseMapper</c> on the expense side: extracted from the use
/// case (<see cref="RegisterSettlementUseCase"/>) to keep edge translation separate from
/// orchestration, and to be the single place that decides the invariants of a new settlement —
/// <see cref="Settlement"/> (T31) is a "dumb" record (no constructor validation), so this mapper is
/// what guarantees no settlement with a zero/negative amount or with payer equal to payee ever
/// gets persisted.
/// </summary>
internal static class SettlementMapper
{
    public static Settlement Build(RegisterSettlementRequest request)
    {
        if (request.AmountCents <= 0)
        {
            throw new ArgumentException("Settlement amount must be positive.", nameof(request));
        }

        if (request.FromParticipantId == request.ToParticipantId)
        {
            throw new ArgumentException(
                "Settlement payer and payee cannot be the same participant.", nameof(request));
        }

        return new Settlement(
            Guid.NewGuid(),
            new ParticipantId(request.FromParticipantId),
            new ParticipantId(request.ToParticipantId),
            Money.FromCents(request.AmountCents));
    }
}

namespace Rateio.Application.Groups;

/// <summary>
/// Persistence of invite codes (T21.1/T21.2). Separate interface from
/// <see cref="IGroupRepository"/> (Interface Segregation, same rationale as
/// <c>Rateio.Application.Expenses.IExpenseRepository</c>): whoever generates/resolves an invite
/// code doesn't need to know about the rest of group persistence. Concrete implementation (EF
/// Core) in Rateio.Infrastructure.
/// </summary>
public interface IInviteCodeRepository
{
    /// <summary>
    /// Persists <paramref name="code"/> as the group's only active code
    /// (<see cref="InviteCode.GroupId"/>) — upsert by group: any previous code from that same
    /// group stops being resolvable by <see cref="GetByCodeAsync"/> after this call (see
    /// <see cref="InviteCode"/> for the justification of "one active code per group").
    /// </summary>
    Task SaveAsync(InviteCode code, CancellationToken cancellationToken = default);

    /// <summary>
    /// Resolves a code to its matching invite, or <c>null</c> if the code never existed or has
    /// already been replaced by a newer one (see <see cref="SaveAsync"/>). Doesn't check
    /// expiration — the caller decides that via <see cref="InviteCode.IsValid"/>, because "code not
    /// found" and "code expired" are two distinct causes of the same HTTP 404, but deserve
    /// different diagnostic messages.
    /// </summary>
    Task<InviteCode?> GetByCodeAsync(string code, CancellationToken cancellationToken = default);
}

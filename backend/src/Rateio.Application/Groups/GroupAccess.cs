namespace Rateio.Application.Groups;

/// <summary>
/// Minimal summary of an already-persisted group, used only to validate RNF07 before an operation
/// on behalf of a user (T23: adding an expense to an already-synced group) — not the full
/// <see cref="Domain.Group"/> aggregate, because whoever calls
/// <see cref="IGroupRepository.GetAccessAsync"/> doesn't need (and shouldn't need) to load
/// participants/expenses just to check access.
///
/// <see cref="OwnerUserId"/> is today the only identity-to-group link T18's schema stores
/// (participants are just name + guest/authenticated flag, with no FK to <c>User</c> — RF06/RF07
/// don't yet model "which Google account an authenticated participant corresponds to"). That's why
/// <see cref="BelongsTo"/> checks the owner, not the participant list; it's the strictest possible
/// reading of RNF07 ("only participants can access") given what exists today — expanding it to
/// authenticated participants is work for a future invite/account-linking task.
/// </summary>
public sealed record GroupAccess(Guid GroupId, Guid OwnerUserId)
{
    public bool BelongsTo(Guid userId) => OwnerUserId == userId;
}

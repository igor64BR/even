namespace Tally.Application.Groups;

/// <summary>
/// Minimal summary of an already-persisted group, used only to validate access before an operation
/// on behalf of a user (e.g. adding an expense to an already-synced group) — not the full
/// <see cref="Domain.Group"/> aggregate, because whoever calls
/// <see cref="IGroupRepository.GetAccessAsync"/> doesn't need (and shouldn't need) to load
/// participants/expenses just to check access.
///
/// <see cref="OwnerUserId"/> is today the only identity-to-group link the schema stores
/// (participants are just name + guest/authenticated flag, with no FK to <c>User</c> — there's no
/// model yet for "which Google account an authenticated participant corresponds to"). That's why
/// <see cref="BelongsTo"/> checks the owner, not the participant list; it's the strictest possible
/// reading of "only participants can access" given what exists today — expanding it to
/// authenticated participants is work for a future invite/account-linking feature.
/// </summary>
public sealed record GroupAccess(Guid GroupId, Guid OwnerUserId)
{
    public bool BelongsTo(Guid userId) => OwnerUserId == userId;
}

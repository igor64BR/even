namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of an invite code (T21.1). The primary key is the <see cref="Code"/> itself
/// (not a synthetic <see cref="Guid"/>): it already needs to be globally unique for
/// <c>POST /groups/join/{code}</c> to resolve unambiguously, so a separate id would only duplicate
/// that guarantee. It has its own table (not a loose column on <c>groups</c>) because the history of
/// "one active code per group, replaced on every generation" (see
/// <c>Rateio.Application.Groups.InviteCode</c>) is more natural as rows in a table than as a single
/// nullable column being overwritten — and it leaves the door open to, in the future, keep
/// expired/revoked codes around without a schema migration.
/// </summary>
public class InviteCodeEntity
{
    public string Code { get; set; } = string.Empty;

    public Guid GroupId { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset ExpiresAt { get; set; }

    public GroupEntity? Group { get; set; }
}

namespace Even.Application.Auth;

/// <summary>
/// User authenticated via Google. Lives in Application (not Domain) because, for now, it's just
/// an identity record with no business rules of its own — if it gains invariants/behavior in the
/// future, it migrates to Even.Domain as an aggregate.
/// </summary>
public sealed record User(Guid Id, string GoogleSubjectId, string Name, string Email);

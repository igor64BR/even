namespace Rateio.Application.Auth;

/// <summary>
/// Usuário autenticado via Google. Vive em Application (não em Domain) porque, por enquanto, é só
/// um registro de identidade sem regra de negócio própria — se ganhar invariantes/comportamento no
/// futuro, migra para Rateio.Domain como um agregado.
/// </summary>
public sealed record Usuario(Guid Id, string GoogleSubjectId, string Nome, string Email);

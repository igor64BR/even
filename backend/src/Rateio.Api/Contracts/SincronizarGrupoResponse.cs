namespace Rateio.Api.Contracts;

/// <summary>
/// Resposta de sucesso de <c>POST /groups/sync</c>. <see cref="GrupoId"/> é o id do grupo no
/// servidor — nunca o id local do Room; o app guarda isso como o "id remoto" do grupo local
/// (escopo de T19, ver T18-backend-sync-grupo.md).
/// </summary>
public sealed record SincronizarGrupoResponse(Guid GrupoId);

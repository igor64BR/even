namespace Rateio.Application.Grupos;

/// <summary>
/// T18.1/T18.2: primeira sincronização de um grupo local pro backend. Constrói
/// <see cref="Domain.Grupo"/>/<see cref="Domain.Participante"/>/<see cref="Domain.Despesa"/> de
/// domínio a partir do payload (via <see cref="ConstrutorDeGrupoSincronizado"/>, que reaproveita
/// <see cref="Domain.Grupo.Criar"/> — não bypassa a invariante de T15), marca o grupo como
/// sincronizado (T18.2: "grupo sincronizado passa a ser fonte da verdade") e persiste via
/// <see cref="IGrupoRepository"/>.
///
/// <paramref name="usuarioAutenticadoId"/> nunca vem do corpo da requisição — o controller extrai
/// do claim <c>sub</c> do JWT validado. É assim que RNF07 é garantido estruturalmente: não existe
/// campo de "dono" no payload, então não há como um usuário sincronizar em nome de outro.
/// </summary>
public sealed class SincronizarGrupoUseCase(IGrupoRepository grupoRepository)
{
    public Task<Guid> ExecutarAsync(
        Guid usuarioAutenticadoId,
        SincronizarGrupoRequest requisicao,
        CancellationToken cancellationToken = default)
    {
        var grupoParaSincronizar = ConstrutorDeGrupoSincronizado.Construir(usuarioAutenticadoId, requisicao);
        grupoParaSincronizar.Grupo.MarcarComoSincronizado();

        return grupoRepository.SincronizarAsync(grupoParaSincronizar, cancellationToken);
    }
}

namespace Rateio.Application.Grupos;

/// <summary>
/// Resumo mínimo de um grupo já persistido, usado só pra validar RNF07 antes de uma operação em
/// nome de um usuário (T23: adicionar despesa a um grupo já sincronizado) — não é o agregado
/// <see cref="Domain.Grupo"/> completo, porque quem chama <see cref="IGrupoRepository.ObterAcessoAsync"/>
/// não precisa (nem deveria precisar) carregar participantes/despesas só pra checar acesso.
///
/// <see cref="DonoUsuarioId"/> é hoje o único vínculo identidade↔grupo que o schema de T18 guarda
/// (participantes são só nome + flag convidado/autenticado, sem FK pra <c>Usuario</c> — RF06/RF07
/// ainda não modelam "participante autenticado corresponde a qual conta Google"). Por isso
/// <see cref="PertenceA"/> checa dono, não a lista de participantes; é a leitura mais estrita
/// possível de RNF07 ("só quem participa acessa") dado o que existe hoje — expandir pra
/// participantes autenticados é trabalho de uma task futura de convite/vínculo de conta.
/// </summary>
public sealed record AcessoAoGrupo(Guid GrupoId, Guid DonoUsuarioId)
{
    public bool PertenceA(Guid usuarioId) => DonoUsuarioId == usuarioId;
}

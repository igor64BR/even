using Rateio.Domain;

namespace Rateio.Application.Grupos;

/// <summary>
/// T21.2: resolve um código de convite pro grupo correspondente e adiciona o usuário autenticado
/// como novo <see cref="Participante"/> autenticado (RF07) — via
/// <see cref="Grupo.AdicionarParticipante"/> (T15, já valida a invariante de "sem participante
/// duplicado"), não construindo a entidade de persistência direto (isso burlaria a validação de
/// domínio, o mesmo cuidado que <c>ConstrutorDeGrupoSincronizado</c>, T18, já toma).
///
/// RNF07/T21: diferente de <see cref="GerarCodigoConviteUseCase"/> (só o dono), entrar via código é
/// aberto a qualquer usuário autenticado — esse é o ponto do recurso (link de convite). Por isso não
/// há checagem de "dono" ou de <see cref="AcessoAoGrupo"/> aqui: o próprio código válido já é a
/// autorização.
///
/// <paramref name="nomeDoUsuarioAutenticado"/> vem do claim <c>name</c> do JWT (o controller
/// extrai) — o mesmo dado que <c>JwtIssuer</c> (T11) grava no token a partir de
/// <c>Usuario.Nome</c>, sem precisar de uma nova consulta a <c>IUsuarioRepository</c> só pra isso.
/// </summary>
public sealed class EntrarNoGrupoViaConviteUseCase(
    ICodigoConviteRepository codigoConviteRepository,
    IGrupoRepository grupoRepository,
    TimeProvider relogio)
{
    public async Task<Guid> ExecutarAsync(
        string codigo,
        string nomeDoUsuarioAutenticado,
        CancellationToken cancellationToken = default)
    {
        var convite = await ObterConviteValidoOuFalhar(codigo, cancellationToken);
        var grupoParaEntrada = await ObterGrupoOuFalhar(convite.GrupoId, cancellationToken);

        var novoParticipante = Participante.Autenticado(
            ParticipanteId.NovoId(),
            NomeParticipante.Criar(nomeDoUsuarioAutenticado));

        // Reconstrói só o suficiente do agregado pra revalidar a invariante de T15 antes de
        // persistir — ver GrupoParaEntrada para por que isso não precisa ser o Grupo completo.
        var grupo = Grupo.Criar(grupoParaEntrada.Nome, grupoParaEntrada.Categoria, grupoParaEntrada.Participantes);
        grupo.AdicionarParticipante(novoParticipante);

        await grupoRepository.AdicionarParticipanteAsync(convite.GrupoId, novoParticipante, cancellationToken);

        return convite.GrupoId;
    }

    private async Task<CodigoConvite> ObterConviteValidoOuFalhar(string codigo, CancellationToken cancellationToken)
    {
        var convite = await codigoConviteRepository.ObterPorCodigoAsync(codigo, cancellationToken)
            ?? throw new CodigoConviteInvalidoException(codigo);

        if (!convite.EstaValido(relogio.GetUtcNow()))
        {
            throw new CodigoConviteInvalidoException(codigo);
        }

        return convite;
    }

    private async Task<GrupoParaEntrada> ObterGrupoOuFalhar(Guid grupoId, CancellationToken cancellationToken) =>
        await grupoRepository.ObterParaEntradaAsync(grupoId, cancellationToken)
            ?? throw new GrupoNaoEncontradoException(grupoId);
}

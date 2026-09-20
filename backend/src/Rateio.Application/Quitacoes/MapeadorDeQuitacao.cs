using Rateio.Domain;

namespace Rateio.Application.Quitacoes;

/// <summary>
/// Mapeamento DTO→Domínio de uma quitação (T35), espelhando
/// <c>Rateio.Application.Despesas.MapeadorDeDespesa</c> do lado da despesa: extraído do caso de uso
/// (<see cref="RegistrarQuitacaoUseCase"/>) pra manter a tradução de borda separada da orquestração,
/// e pra ser o único lugar que decide as invariantes de uma quitação nova — <see cref="Quitacao"/>
/// (T31) é um record "burro" (sem validação no construtor), então é este mapeador que garante que
/// nenhuma quitação com valor zero/negativo ou pagador igual ao recebedor chega a ser persistida.
/// </summary>
internal static class MapeadorDeQuitacao
{
    public static Quitacao Construir(RegistrarQuitacaoRequest requisicao)
    {
        if (requisicao.ValorCentavos <= 0)
        {
            throw new ArgumentException("Valor da quitação deve ser positivo.", nameof(requisicao));
        }

        if (requisicao.DeParticipanteId == requisicao.ParaParticipanteId)
        {
            throw new ArgumentException(
                "Pagador e recebedor da quitação não podem ser o mesmo participante.", nameof(requisicao));
        }

        return new Quitacao(
            Guid.NewGuid(),
            new ParticipanteId(requisicao.DeParticipanteId),
            new ParticipanteId(requisicao.ParaParticipanteId),
            Dinheiro.EmCentavos(requisicao.ValorCentavos));
    }
}

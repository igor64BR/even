namespace Rateio.Application.Notificacoes;

/// <summary>
/// Disparado por <see cref="Quitacoes.RegistrarQuitacaoUseCase"/> (T35) depois de persistir com
/// sucesso (T38.3). Carrega de/para/valor — o suficiente pra o app refletir a quitação na UI sem
/// round-trip adicional (RNF10: entrega em ≤2s); a próxima leitura de
/// <c>GET /groups/{id}/settlement</c> continua sendo a fonte de verdade recomputada.
/// </summary>
public sealed record EventoDividaQuitada(
    Guid GrupoId,
    Guid QuitacaoId,
    Guid DeParticipanteId,
    Guid ParaParticipanteId,
    long ValorCentavos) : IEventoDeGrupo
{
    public TipoEventoDeGrupo Tipo => TipoEventoDeGrupo.DividaQuitada;
}

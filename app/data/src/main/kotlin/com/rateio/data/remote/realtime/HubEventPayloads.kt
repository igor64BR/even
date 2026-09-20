package com.rateio.data.remote.realtime

/**
 * Payload do método SignalR "DespesaCriada" — espelha `EventoDespesaCriada`
 * (`Rateio.Application.Notificacoes`, backend T38.2/T38's `NotificadorDeEventoDeGrupoSignalR`, que
 * manda `nameof(TipoEventoDeGrupo.DespesaCriada)` como nome do método com o record inteiro como
 * argumento). O client Java oficial do SignalR (`com.microsoft.signalr:signalr`) desserializa
 * esses payloads com Gson por baixo dos panos (confirmado no POM publicado do artefato — depende
 * de `com.google.code.gson:gson`, não Jackson): Gson preenche os campos via reflection direta, sem
 * exigir getter/setter nem construtor específico, então um `data class` idiomático com os mesmos
 * nomes de campo do JSON (camelCase — `JsonHubProtocol` padrão do ASP.NET Core sem conversor
 * customizado) já funciona sem anotação nenhuma. Valores default defendem contra um payload
 * parcial nunca travando a desserialização.
 */
internal data class DespesaCriadaPayload(
    val grupoId: String = "",
    val despesaId: String = "",
    val descricao: String = "",
    val valorTotalCentavos: Long = 0,
    val pagadorId: String = "",
)

/** Payload do método SignalR "DividaQuitada" — espelha `EventoDividaQuitada`. Ver [DespesaCriadaPayload]. */
internal data class DividaQuitadaPayload(
    val grupoId: String = "",
    val quitacaoId: String = "",
    val deParticipanteId: String = "",
    val paraParticipanteId: String = "",
    val valorCentavos: Long = 0,
)

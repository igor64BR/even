package com.rateio.domain.model

/**
 * Participante de um grupo. O caso padrão é o convidado sem conta — [name] é o único dado
 * obrigatório, em linha com o princípio local-first da constitution: nenhum campo de
 * autenticação é exigido para alguém existir como participante.
 *
 * [isYou] marca qual participante corresponde ao dono deste dispositivo, para a UI destacar
 * "Você" nas listas — mesma convenção do protótipo (ver `prototype/app.js`, campo `isYou`).
 */
data class Participant(
    val id: String,
    val groupId: String,
    val name: String,
    val isYou: Boolean = false,
)

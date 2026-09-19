package com.rateio.domain.model

import java.time.Instant

/**
 * Grupo de despesas compartilhadas. A fonte da verdade é sempre o dispositivo enquanto o grupo
 * não é sincronizado (constitution.md, princípio 1) — nenhum campo aqui depende de conta ou
 * rede para existir.
 */
data class Group(
    val id: String,
    val name: String,
    val createdAt: Instant,
)

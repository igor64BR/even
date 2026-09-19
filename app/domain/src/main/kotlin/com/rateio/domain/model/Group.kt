package com.rateio.domain.model

import java.time.Instant

/**
 * Grupo de despesas compartilhadas. A fonte da verdade é sempre o dispositivo enquanto o grupo
 * não é sincronizado (constitution.md, princípio 1) — nenhum campo aqui depende de conta ou
 * rede para existir.
 *
 * [isSynced] espelha `Sincronizado` de `Grupo.cs` (backend): `false` para todo grupo local recém
 * criado, só vira `true` quando o grupo é sincronizado com o backend (RF09). `Grupo.cs` protege
 * essa transição com um setter privado + método `MarcarComoSincronizado()`, porque lá `Grupo` é
 * um agregado com invariante própria (mínimo de 1 participante) e sem setters soltos. Este
 * `Group` continua o `data class` simples que T7 já modelou (sem invariante de participantes —
 * isso vive em `Grupo.cs`/futura task de agregado rico no Android, fora do escopo de T7B), então
 * a mesma transição de estado aqui é `group.copy(isSynced = true)`: idiomático em Kotlin e sem
 * abrir nenhum caminho novo pra mutar o campo por fora (o `data class` já é imutável).
 */
data class Group(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val isSynced: Boolean = false,
)

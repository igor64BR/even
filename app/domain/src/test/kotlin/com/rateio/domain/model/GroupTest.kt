package com.rateio.domain.model

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupTest {

    @Test
    fun `grupo novo nasce nao sincronizado por padrao`() {
        val group = Group(id = "g1", name = "Churras", createdAt = Instant.EPOCH)

        assertFalse(group.isSynced)
    }

    @Test
    fun `marcar como sincronizado eh copy com isSynced=true, sem mutar o original`() {
        // Group e um data class imutavel (T7) -- diferente de Grupo.cs (agregado com setter
        // privado + MarcarComoSincronizado()), a transicao aqui e a copy() que o data class ja
        // da de graca, sem precisar de um metodo novo que so delegaria pra copy() mesmo.
        val local = Group(id = "g1", name = "Churras", createdAt = Instant.EPOCH)

        val synced = local.copy(isSynced = true)

        assertFalse(local.isSynced)
        assertTrue(synced.isSynced)
    }
}

package com.even.domain.model

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupTest {

    @Test
    fun `a new group starts out not synced by default`() {
        val group = Group(id = "g1", name = "Barbecue", createdAt = Instant.EPOCH)

        assertFalse(group.isSynced)
    }

    @Test
    fun `marking as synced is a copy with isSynced=true, without mutating the original`() {
        // Group is an immutable data class -- unlike Group.cs (an aggregate with a private
        // setter + MarkAsSynced()), the transition here is the copy() the data class already
        // gives for free, with no need for a new method that would just delegate to copy() anyway.
        val local = Group(id = "g1", name = "Barbecue", createdAt = Instant.EPOCH)

        val synced = local.copy(isSynced = true)

        assertFalse(local.isSynced)
        assertTrue(synced.isSynced)
    }

    @Test
    fun `a new group starts with no remoteId, syncing fills in both fields together`() {
        // isSynced and remoteId always travel together -- never one filled without the other.
        val local = Group(id = "g1", name = "Barbecue", createdAt = Instant.EPOCH)

        assertTrue(local.remoteId == null)

        val synced = local.copy(isSynced = true, remoteId = "server-id")

        assertTrue(synced.isSynced && synced.remoteId == "server-id")
    }
}

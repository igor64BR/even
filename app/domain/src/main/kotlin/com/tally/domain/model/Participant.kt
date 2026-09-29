package com.tally.domain.model

/**
 * A group participant. The default case is a guest with no account — [name] is the only required
 * piece of data, in line with the constitution's local-first principle: no authentication field
 * is required for someone to exist as a participant.
 *
 * [isYou] marks which participant corresponds to this device's owner, so the UI can highlight
 * "You" in lists — same convention as the prototype (see `prototype/app.js`, the `isYou` field).
 */
data class Participant(
    val id: String,
    val groupId: String,
    val name: String,
    val isYou: Boolean = false,
)

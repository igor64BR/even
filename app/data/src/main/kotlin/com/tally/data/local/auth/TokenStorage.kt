package com.tally.data.local.auth

import com.tally.domain.model.AuthSession

/**
 * Local persistence of the [AuthSession] (T12.2). A separate interface from
 * [com.tally.data.repository.RemoteAuthRepository] for single responsibility: the repository
 * orchestrates network + state, the actual storage (and the decision of how to encrypt it) belongs
 * only to this piece — see [EncryptedTokenStorage].
 */
interface TokenStorage {
    fun read(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

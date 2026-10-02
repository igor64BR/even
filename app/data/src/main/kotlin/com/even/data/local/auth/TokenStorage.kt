package com.even.data.local.auth

import com.even.domain.model.AuthSession

/**
 * Local persistence of the [AuthSession]. A separate interface from
 * [com.even.data.repository.RemoteAuthRepository] for single responsibility: the repository
 * orchestrates network + state, the actual storage (and the decision of how to encrypt it) belongs
 * only to this piece — see [EncryptedTokenStorage].
 */
interface TokenStorage {
    fun read(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

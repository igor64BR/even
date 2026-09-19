package com.rateio.data.local.auth

import com.rateio.domain.model.AuthSession

/**
 * Persistência local da [AuthSession] (T12.2). Interface separada de
 * [com.rateio.data.repository.RemoteAuthRepository] por responsabilidade única: o repositório
 * orquestra rede + estado, o armazenamento em si (e a decisão de como cifrar) é só desta peça —
 * ver [EncryptedTokenStorage].
 */
interface TokenStorage {
    fun read(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

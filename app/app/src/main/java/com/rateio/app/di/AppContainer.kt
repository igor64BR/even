package com.rateio.app.di

import android.content.Context
import androidx.room.Room
import com.rateio.app.BuildConfig
import com.rateio.app.auth.GoogleIdentityClient
import com.rateio.data.local.auth.EncryptedTokenStorage
import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.remote.RateioHttpClientFactory
import com.rateio.data.remote.auth.AuthApi
import com.rateio.data.repository.RemoteAuthRepository
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomParticipantRepository
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository

/**
 * Raiz de composição manual do módulo `:app` — não há framework de DI no projeto ainda. Monta o
 * Room de `:data` uma única vez por processo e expõe só os contratos de `:domain` que as telas
 * consomem (Dependency Inversion: telas dependem de interface, não de `RoomGroupRepository`).
 */
class AppContainer(context: Context) {

    private val database: RateioDatabase = Room.databaseBuilder(
        context.applicationContext,
        RateioDatabase::class.java,
        DATABASE_NAME,
    ).build()

    val groupRepository: GroupRepository by lazy { RoomGroupRepository(database.groupDao()) }
    val participantRepository: ParticipantRepository by lazy { RoomParticipantRepository(database.participantDao()) }

    // T12 — autenticação opcional via Google (constitution.md, princípios 1 e 2). `authApi`
    // usa BuildConfig.API_BASE_URL (placeholder documentado em `app/app/build.gradle.kts`);
    // `tokenStorage` cifra a sessão no aparelho (EncryptedSharedPreferences, T12.2).
    private val tokenStorage: TokenStorage by lazy { EncryptedTokenStorage(context.applicationContext) }
    private val authApi: AuthApi by lazy { RateioHttpClientFactory.createAuthApi(BuildConfig.API_BASE_URL) }
    val authRepository: AuthRepository by lazy { RemoteAuthRepository(authApi, tokenStorage) }
    val googleIdentityClient: GoogleIdentityClient by lazy { GoogleIdentityClient(context.applicationContext) }

    private companion object {
        const val DATABASE_NAME = "rateio.db"
    }
}

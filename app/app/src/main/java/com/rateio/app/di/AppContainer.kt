package com.rateio.app.di

import android.content.Context
import androidx.room.Room
import com.rateio.app.BuildConfig
import com.rateio.app.auth.GoogleIdentityClient
import com.rateio.app.ui.format.AppMoneyFormatter
import com.rateio.data.local.auth.EncryptedTokenStorage
import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.remote.RateioHttpClientFactory
import com.rateio.data.remote.auth.AuthApi
import com.rateio.data.remote.groups.GroupEventsApi
import com.rateio.data.remote.groups.GroupsApi
import com.rateio.data.remote.realtime.SignalRGroupRealtimeGateway
import com.rateio.data.repository.RemoteAuthRepository
import com.rateio.data.repository.RemoteExpenseSyncRepository
import com.rateio.data.repository.RemoteGroupSyncRepository
import com.rateio.data.repository.RoomExpenseRepository
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomNotificationRepository
import com.rateio.data.repository.RoomParticipantRepository
import com.rateio.data.repository.RoomSettlementRepository
import com.rateio.domain.engine.DebtSimplificationEngine
import com.rateio.domain.engine.GreedyDebtSimplificationEngine
import com.rateio.domain.format.MoneyFormatter
import com.rateio.domain.realtime.GroupRealtimeGateway
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.NotificationRepository
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.RemoteExpenseRepository
import com.rateio.domain.repository.RemoteGroupRepository
import com.rateio.domain.repository.SettlementRepository

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
    val expenseRepository: ExpenseRepository by lazy { RoomExpenseRepository(database.expenseDao()) }

    // T42.1 — persistência de quitações (RF31/RF33), consumida pela tela "Quitar dívidas" (T42.3).
    val settlementRepository: SettlementRepository by lazy { RoomSettlementRepository(database.settlementDao()) }

    // T33 — motor de simplificação de dívidas (algorithm-spec.md). Interface de :domain, única
    // implementação também vive em :domain (GreedyDebtSimplificationEngine) — não é um contrato de
    // persistência como os `Room*Repository` acima, mas segue a mesma composição manual: quem
    // consome (T42.2/T42.3) depende da interface, não da classe concreta.
    val debtSimplificationEngine: DebtSimplificationEngine by lazy { GreedyDebtSimplificationEngine() }

    // T12 — autenticação opcional via Google (constitution.md, princípios 1 e 2). `authApi`
    // usa BuildConfig.API_BASE_URL (placeholder documentado em `app/app/build.gradle.kts`);
    // `tokenStorage` cifra a sessão no aparelho (EncryptedSharedPreferences, T12.2).
    private val tokenStorage: TokenStorage by lazy { EncryptedTokenStorage(context.applicationContext) }
    private val authApi: AuthApi by lazy { RateioHttpClientFactory.createAuthApi(BuildConfig.API_BASE_URL) }
    val authRepository: AuthRepository by lazy { RemoteAuthRepository(authApi, tokenStorage) }
    val googleIdentityClient: GoogleIdentityClient by lazy { GoogleIdentityClient(context.applicationContext) }

    // T19 — ação "Sincronizar este grupo" (POST /groups/sync, T18). Reaproveita o mesmo
    // `tokenStorage` de T12 pra ler o access token na hora de montar o header Authorization.
    private val groupsApi: GroupsApi by lazy { RateioHttpClientFactory.createGroupsApi(BuildConfig.API_BASE_URL) }
    val remoteGroupRepository: RemoteGroupRepository by lazy { RemoteGroupSyncRepository(groupsApi, tokenStorage) }

    // T29 — propagação de edição/exclusão de despesa pro backend quando o grupo está sincronizado
    // (`PUT`/`DELETE /groups/{id}/expenses/{expenseId}`, T28). Mesmo `groupsApi`/`tokenStorage` de
    // remoteGroupRepository, interface própria por responsabilidade única (ver KDoc de
    // RemoteExpenseRepository).
    val remoteExpenseRepository: RemoteExpenseRepository by lazy { RemoteExpenseSyncRepository(groupsApi, tokenStorage) }

    // T40/T41 — notificações locais (Room) + cliente SignalR (T38's Hub) + fallback de pull (T39).
    val notificationRepository: NotificationRepository by lazy { RoomNotificationRepository(database.notificationDao()) }

    // GroupEventsApi.getEvents é o pull de T39; RateioHttpClientFactory reaproveita o mesmo
    // Retrofit.Builder de authApi/groupsApi (ver KDoc da fábrica).
    private val groupEventsApi: GroupEventsApi by lazy {
        RateioHttpClientFactory.createGroupEventsApi(BuildConfig.API_BASE_URL)
    }

    // com.rateio.domain.format.MoneyFormatter é uma abstração de :domain que :data precisa pra
    // montar o texto da notificação sem depender de java.text.NumberFormat/Locale diretamente —
    // ver KDoc de MoneyFormatter. AppMoneyFormatter é o único lugar do app que faz essa ponte.
    private val moneyFormatter: MoneyFormatter by lazy { AppMoneyFormatter() }

    // hubUrl reaproveita o mesmo host/porta de API_BASE_URL (BuildConfig.API_BASE_URL já termina
    // em "/", ver comentário do placeholder em app/app/build.gradle.kts) — RotaDoHubDeNotificacoes
    // no backend mapeia o Hub em "/hubs/rateio" sobre o mesmo servidor da API REST.
    val groupRealtimeGateway: GroupRealtimeGateway by lazy {
        SignalRGroupRealtimeGateway(
            hubUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/hubs/rateio",
            tokenStorage = tokenStorage,
            notificationRepository = notificationRepository,
            groupRepository = groupRepository,
            participantRepository = participantRepository,
            groupEventsApi = groupEventsApi,
            moneyFormatter = moneyFormatter,
        )
    }

    private companion object {
        const val DATABASE_NAME = "rateio.db"
    }
}

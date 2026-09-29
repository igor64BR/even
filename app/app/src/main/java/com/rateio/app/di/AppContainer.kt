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
import com.rateio.data.repository.SharedPreferencesThemeRepository
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
import com.rateio.domain.repository.ThemeRepository

/**
 * Manual composition root for the `:app` module — there's no DI framework in the project yet.
 * Builds `:data`'s Room once per process and exposes only the `:domain` contracts the screens
 * consume (Dependency Inversion: screens depend on the interface, not on `RoomGroupRepository`).
 */
class AppContainer(context: Context) {

    private val database: RateioDatabase = Room.databaseBuilder(
        context.applicationContext,
        RateioDatabase::class.java,
        DATABASE_NAME,
    ).build()

    // Light/dark theme preference (the sun/moon button in the TopAppBar on every screen) — not
    // business-domain data like the rest below, it's only up here because it's the only dependency
    // MainActivity needs to resolve before even setting up `RateioTheme`.
    val themeRepository: ThemeRepository by lazy { SharedPreferencesThemeRepository(context.applicationContext) }

    val groupRepository: GroupRepository by lazy { RoomGroupRepository(database.groupDao()) }
    val participantRepository: ParticipantRepository by lazy { RoomParticipantRepository(database.participantDao()) }
    val expenseRepository: ExpenseRepository by lazy { RoomExpenseRepository(database.expenseDao()) }

    // T42.1 — settlement persistence (RF31/RF33), consumed by the "Settle debts" screen (T42.3).
    val settlementRepository: SettlementRepository by lazy { RoomSettlementRepository(database.settlementDao()) }

    // T33 — the debt-simplification engine (algorithm-spec.md). A :domain interface, its only
    // implementation also lives in :domain (GreedyDebtSimplificationEngine) — not a persistence
    // contract like the `Room*Repository`s above, but follows the same manual composition: the
    // consumer (T42.2/T42.3) depends on the interface, not the concrete class.
    val debtSimplificationEngine: DebtSimplificationEngine by lazy { GreedyDebtSimplificationEngine() }

    // T12 — optional authentication via Google (constitution.md, principles 1 and 2). `authApi`
    // uses BuildConfig.API_BASE_URL (placeholder documented in `app/app/build.gradle.kts`);
    // `tokenStorage` encrypts the session on the device (EncryptedSharedPreferences, T12.2).
    private val tokenStorage: TokenStorage by lazy { EncryptedTokenStorage(context.applicationContext) }
    private val authApi: AuthApi by lazy { RateioHttpClientFactory.createAuthApi(BuildConfig.API_BASE_URL) }
    val authRepository: AuthRepository by lazy { RemoteAuthRepository(authApi, tokenStorage) }
    val googleIdentityClient: GoogleIdentityClient by lazy { GoogleIdentityClient(context.applicationContext) }

    // T19 — the "Sync this group" action (POST /groups/sync, T18). Reuses the same `tokenStorage`
    // from T12 to read the access token when building the Authorization header.
    private val groupsApi: GroupsApi by lazy { RateioHttpClientFactory.createGroupsApi(BuildConfig.API_BASE_URL) }
    val remoteGroupRepository: RemoteGroupRepository by lazy { RemoteGroupSyncRepository(groupsApi, tokenStorage) }

    // T29 — propagating an expense edit/delete to the backend when the group is synced
    // (`PUT`/`DELETE /groups/{id}/expenses/{expenseId}`, T28). Same `groupsApi`/`tokenStorage` as
    // remoteGroupRepository, its own interface for single responsibility (see the KDoc of
    // RemoteExpenseRepository).
    val remoteExpenseRepository: RemoteExpenseRepository by lazy { RemoteExpenseSyncRepository(groupsApi, tokenStorage) }

    // T40/T41 — local notifications (Room) + SignalR client (T38's Hub) + pull fallback (T39).
    val notificationRepository: NotificationRepository by lazy { RoomNotificationRepository(database.notificationDao()) }

    // GroupEventsApi.getEvents is T39's pull; RateioHttpClientFactory reuses the same
    // Retrofit.Builder as authApi/groupsApi (see the factory's KDoc).
    private val groupEventsApi: GroupEventsApi by lazy {
        RateioHttpClientFactory.createGroupEventsApi(BuildConfig.API_BASE_URL)
    }

    // com.rateio.domain.format.MoneyFormatter is a :domain abstraction :data needs to build the
    // notification text without depending on java.text.NumberFormat/Locale directly — see
    // MoneyFormatter's KDoc. AppMoneyFormatter is the app's only bridge for this.
    private val moneyFormatter: MoneyFormatter by lazy { AppMoneyFormatter() }

    // hubUrl reuses the same host/port as API_BASE_URL (BuildConfig.API_BASE_URL already ends in
    // "/", see the placeholder comment in app/app/build.gradle.kts) — NotificationHubRoute on the
    // backend maps the Hub to "/hubs/rateio" on the same server as the REST API.
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

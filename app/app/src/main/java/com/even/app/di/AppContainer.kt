package com.even.app.di

import android.content.Context
import androidx.room.Room
import com.even.app.BuildConfig
import com.even.app.auth.GoogleIdentityClient
import com.even.app.ui.format.AppMoneyFormatter
import com.even.data.local.auth.EncryptedTokenStorage
import com.even.data.local.auth.TokenStorage
import com.even.data.persistence.EvenDatabase
import com.even.data.remote.EvenHttpClientFactory
import com.even.data.remote.auth.AuthApi
import com.even.data.remote.groups.GroupEventsApi
import com.even.data.remote.groups.GroupsApi
import com.even.data.remote.realtime.SignalRGroupRealtimeGateway
import com.even.data.repository.RemoteAuthRepository
import com.even.data.repository.RemoteExpenseSyncRepository
import com.even.data.repository.RemoteGroupSyncRepository
import com.even.data.repository.RoomExpenseRepository
import com.even.data.repository.RoomGroupRepository
import com.even.data.repository.RoomNotificationRepository
import com.even.data.repository.RoomParticipantRepository
import com.even.data.repository.RoomSettlementRepository
import com.even.data.repository.SharedPreferencesThemeRepository
import com.even.domain.engine.DebtSimplificationEngine
import com.even.domain.engine.GreedyDebtSimplificationEngine
import com.even.domain.format.MoneyFormatter
import com.even.domain.realtime.GroupRealtimeGateway
import com.even.domain.repository.AuthRepository
import com.even.domain.repository.ExpenseRepository
import com.even.domain.repository.GroupRepository
import com.even.domain.repository.NotificationRepository
import com.even.domain.repository.ParticipantRepository
import com.even.domain.repository.RemoteExpenseRepository
import com.even.domain.repository.RemoteGroupRepository
import com.even.domain.repository.SettlementRepository
import com.even.domain.repository.ThemeRepository

/**
 * Manual composition root for the `:app` module — there's no DI framework in the project yet.
 * Builds `:data`'s Room once per process and exposes only the `:domain` contracts the screens
 * consume (Dependency Inversion: screens depend on the interface, not on `RoomGroupRepository`).
 */
class AppContainer(context: Context) {

    private val database: EvenDatabase = Room.databaseBuilder(
        context.applicationContext,
        EvenDatabase::class.java,
        DATABASE_NAME,
    ).build()

    // Theme preference, system/light/dark (the sun/moon button in the TopAppBar on every screen) — not
    // business-domain data like the rest below, it's only up here because it's the only dependency
    // MainActivity needs to resolve before even setting up `EvenTheme`.
    val themeRepository: ThemeRepository by lazy { SharedPreferencesThemeRepository(context.applicationContext) }

    val groupRepository: GroupRepository by lazy { RoomGroupRepository(database.groupDao()) }
    val participantRepository: ParticipantRepository by lazy { RoomParticipantRepository(database.participantDao()) }
    val expenseRepository: ExpenseRepository by lazy { RoomExpenseRepository(database.expenseDao()) }

    // Settlement persistence, consumed by the "Settle debts" screen.
    val settlementRepository: SettlementRepository by lazy { RoomSettlementRepository(database.settlementDao()) }

    // The debt-simplification engine. A :domain interface, its only implementation also lives in
    // :domain (GreedyDebtSimplificationEngine) — not a persistence contract like the
    // `Room*Repository`s above, but follows the same manual composition: the consumer depends on
    // the interface, not the concrete class.
    val debtSimplificationEngine: DebtSimplificationEngine by lazy { GreedyDebtSimplificationEngine() }

    // Optional authentication via Google. `authApi` uses BuildConfig.API_BASE_URL (placeholder
    // documented in `app/app/build.gradle.kts`); `tokenStorage` encrypts the session on the device
    // (EncryptedSharedPreferences).
    private val tokenStorage: TokenStorage by lazy { EncryptedTokenStorage(context.applicationContext) }
    private val authApi: AuthApi by lazy { EvenHttpClientFactory.createAuthApi(BuildConfig.API_BASE_URL) }
    val authRepository: AuthRepository by lazy { RemoteAuthRepository(authApi, tokenStorage) }
    val googleIdentityClient: GoogleIdentityClient by lazy { GoogleIdentityClient(context.applicationContext) }

    // The "Sync this group" action (POST /groups/sync). Reuses the same `tokenStorage` to read the
    // access token when building the Authorization header.
    private val groupsApi: GroupsApi by lazy { EvenHttpClientFactory.createGroupsApi(BuildConfig.API_BASE_URL) }
    val remoteGroupRepository: RemoteGroupRepository by lazy { RemoteGroupSyncRepository(groupsApi, tokenStorage) }

    // Propagates an expense edit/delete to the backend when the group is synced
    // (`PUT`/`DELETE /groups/{id}/expenses/{expenseId}`). Same `groupsApi`/`tokenStorage` as
    // remoteGroupRepository, its own interface for single responsibility (see the KDoc of
    // RemoteExpenseRepository).
    val remoteExpenseRepository: RemoteExpenseRepository by lazy { RemoteExpenseSyncRepository(groupsApi, tokenStorage) }

    // Local notifications (Room) + SignalR client (Hub) + pull fallback.
    val notificationRepository: NotificationRepository by lazy { RoomNotificationRepository(database.notificationDao()) }

    // GroupEventsApi.getEvents is the pull fallback; EvenHttpClientFactory reuses the same
    // Retrofit.Builder as authApi/groupsApi (see the factory's KDoc).
    private val groupEventsApi: GroupEventsApi by lazy {
        EvenHttpClientFactory.createGroupEventsApi(BuildConfig.API_BASE_URL)
    }

    // com.even.domain.format.MoneyFormatter is a :domain abstraction :data needs to build the
    // notification text without depending on java.text.NumberFormat/Locale directly — see
    // MoneyFormatter's KDoc. AppMoneyFormatter is the app's only bridge for this.
    private val moneyFormatter: MoneyFormatter by lazy { AppMoneyFormatter() }

    // hubUrl reuses the same host/port as API_BASE_URL (BuildConfig.API_BASE_URL already ends in
    // "/", see the placeholder comment in app/app/build.gradle.kts) — NotificationHubRoute on the
    // backend maps the Hub to "/hubs/even" on the same server as the REST API.
    val groupRealtimeGateway: GroupRealtimeGateway by lazy {
        SignalRGroupRealtimeGateway(
            hubUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/hubs/even",
            tokenStorage = tokenStorage,
            notificationRepository = notificationRepository,
            groupRepository = groupRepository,
            participantRepository = participantRepository,
            groupEventsApi = groupEventsApi,
            moneyFormatter = moneyFormatter,
        )
    }

    private companion object {
        const val DATABASE_NAME = "even.db"
    }
}

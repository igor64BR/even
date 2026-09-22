package com.rateio.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Preferência de tema claro/escuro escolhida manualmente pelo usuário (o botão sol/lua no
 * `TopAppBar` de toda tela — mesmo componente global de `prototype/app.js`'s `initThemeToggle`).
 *
 * `:domain` declara, `:data` implementa sobre `SharedPreferences` (Dependency Inversion) — mesmo
 * padrão de [NotificationRepository]/[SettlementRepository]: nenhum tipo de Android vaza pra esta
 * interface.
 */
interface ThemeRepository {

    /**
     * `null` enquanto o usuário nunca tocou no botão (nenhuma preferência salva ainda) — quem
     * consome decide o padrão nesse caso (o app usa o tema do sistema, `isSystemInDarkTheme()`,
     * mesma ideia do protótipo abrir no tema salvo ou "claro" se não houver nada salvo). Depois do
     * primeiro toque, sempre um valor explícito, que passa a valer em todo lançamento futuro do
     * app até o usuário tocar de novo.
     */
    fun getIsDarkThemeFlow(): Flow<Boolean?>

    suspend fun setDarkTheme(isDarkTheme: Boolean)
}

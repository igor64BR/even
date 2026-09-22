package com.rateio.data.repository

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric (mesmo padrão de `RateioDatabaseTest`) — `SharedPreferences` exige um `Context` real. */
@RunWith(RobolectricTestRunner::class)
class SharedPreferencesThemeRepositoryTest {

    @Test
    fun `sem preferencia salva, getIsDarkThemeFlow comeca null`() = runTest {
        val repository = SharedPreferencesThemeRepository(ApplicationProvider.getApplicationContext())

        assertNull(repository.getIsDarkThemeFlow().value)
    }

    @Test
    fun `setDarkTheme atualiza o flow imediatamente`() = runTest {
        val repository = SharedPreferencesThemeRepository(ApplicationProvider.getApplicationContext())

        repository.setDarkTheme(true)

        assertEquals(true, repository.getIsDarkThemeFlow().value)
    }

    @Test
    fun `preferencia sobrevive a uma nova instancia (persistida em disco)`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SharedPreferencesThemeRepository(context).setDarkTheme(true)

        val reloaded = SharedPreferencesThemeRepository(context)

        assertEquals(true, reloaded.getIsDarkThemeFlow().value)
    }

    @Test
    fun `alternar duas vezes volta ao valor original`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SharedPreferencesThemeRepository(context)

        repository.setDarkTheme(true)
        repository.setDarkTheme(false)

        assertEquals(false, repository.getIsDarkThemeFlow().value)
    }
}

package com.rateio.data.repository

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric (same pattern as `RateioDatabaseTest`) — `SharedPreferences` requires a real `Context`. */
@RunWith(RobolectricTestRunner::class)
class SharedPreferencesThemeRepositoryTest {

    @Test
    fun `with no saved preference, getIsDarkThemeFlow starts null`() = runTest {
        val repository = SharedPreferencesThemeRepository(ApplicationProvider.getApplicationContext())

        assertNull(repository.getIsDarkThemeFlow().value)
    }

    @Test
    fun `setDarkTheme updates the flow immediately`() = runTest {
        val repository = SharedPreferencesThemeRepository(ApplicationProvider.getApplicationContext())

        repository.setDarkTheme(true)

        assertEquals(true, repository.getIsDarkThemeFlow().value)
    }

    @Test
    fun `the preference survives a new instance (persisted to disk)`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SharedPreferencesThemeRepository(context).setDarkTheme(true)

        val reloaded = SharedPreferencesThemeRepository(context)

        assertEquals(true, reloaded.getIsDarkThemeFlow().value)
    }

    @Test
    fun `toggling twice returns to the original value`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SharedPreferencesThemeRepository(context)

        repository.setDarkTheme(true)
        repository.setDarkTheme(false)

        assertEquals(false, repository.getIsDarkThemeFlow().value)
    }
}

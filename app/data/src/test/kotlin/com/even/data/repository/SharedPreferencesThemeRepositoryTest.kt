package com.even.data.repository

import androidx.test.core.app.ApplicationProvider
import com.even.domain.model.ThemePreference
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric (same pattern as `EvenDatabaseTest`) — `SharedPreferences` requires a real `Context`. */
@RunWith(RobolectricTestRunner::class)
class SharedPreferencesThemeRepositoryTest {

    @Test
    fun `with no saved preference, getThemePreferenceFlow starts on SYSTEM`() = runTest {
        val repository = SharedPreferencesThemeRepository(ApplicationProvider.getApplicationContext())

        assertEquals(ThemePreference.SYSTEM, repository.getThemePreferenceFlow().value)
    }

    @Test
    fun `setThemePreference updates the flow immediately`() = runTest {
        val repository = SharedPreferencesThemeRepository(ApplicationProvider.getApplicationContext())

        repository.setThemePreference(ThemePreference.DARK)

        assertEquals(ThemePreference.DARK, repository.getThemePreferenceFlow().value)
    }

    @Test
    fun `the preference survives a new instance (persisted to disk)`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SharedPreferencesThemeRepository(context).setThemePreference(ThemePreference.DARK)

        val reloaded = SharedPreferencesThemeRepository(context)

        assertEquals(ThemePreference.DARK, reloaded.getThemePreferenceFlow().value)
    }

    @Test
    fun `toggling twice returns to the original value`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SharedPreferencesThemeRepository(context)

        repository.setThemePreference(ThemePreference.DARK)
        repository.setThemePreference(ThemePreference.LIGHT)

        assertEquals(ThemePreference.LIGHT, repository.getThemePreferenceFlow().value)
    }

    @Test
    fun `SYSTEM is an explicit choice, not just the absence of one`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SharedPreferencesThemeRepository(context)
        repository.setThemePreference(ThemePreference.DARK)

        repository.setThemePreference(ThemePreference.SYSTEM)

        assertEquals(ThemePreference.SYSTEM, repository.getThemePreferenceFlow().value)
        assertEquals(ThemePreference.SYSTEM, SharedPreferencesThemeRepository(context).getThemePreferenceFlow().value)
    }

    @Test
    fun `an unrecognized stored value falls back to SYSTEM`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("even_ui_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("theme_preference", "SEPIA")
            .commit()

        val repository = SharedPreferencesThemeRepository(context)

        assertEquals(ThemePreference.SYSTEM, repository.getThemePreferenceFlow().value)
    }
}

package com.even.app.ui.theme

import com.even.domain.model.ThemePreference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The default (SYSTEM) must follow the device; an explicit choice must override it. */
class ThemePreferenceResolutionTest {

    @Test
    fun `SYSTEM follows the device in dark mode`() {
        assertTrue(ThemePreference.SYSTEM.resolveIsDarkTheme(systemInDarkTheme = true))
    }

    @Test
    fun `SYSTEM follows the device in light mode`() {
        assertFalse(ThemePreference.SYSTEM.resolveIsDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun `DARK ignores a device in light mode`() {
        assertTrue(ThemePreference.DARK.resolveIsDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun `LIGHT ignores a device in dark mode`() {
        assertFalse(ThemePreference.LIGHT.resolveIsDarkTheme(systemInDarkTheme = true))
    }
}

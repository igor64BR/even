package com.tally.app

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers T22.1: [extractInviteCode] is the only new deep-link parsing logic for
 * `tally://join/{code}` (`AndroidManifest.xml`) — needs a real `Intent`/`Uri` (Robolectric), the
 * same runner as `GroupDetailViewModelTest`.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityDeepLinkTest {

    @Test
    fun `a VIEW intent with the deep link extracts the invite code`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tally://join/ABC123"))

        assertEquals("ABC123", intent.extractInviteCode())
    }

    @Test
    fun `a normal launcher-open intent has no invite code`() {
        val intent = Intent(Intent.ACTION_MAIN)

        assertNull(intent.extractInviteCode())
    }

    @Test
    fun `a different scheme is not recognized as the app's deep link`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/join/ABC123"))

        assertNull(intent.extractInviteCode())
    }

    @Test
    fun `a host other than join is not recognized`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tally://somethingelse/ABC123"))

        assertNull(intent.extractInviteCode())
    }

    @Test
    fun `a link with no code in the path returns null`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tally://join/"))

        assertNull(intent.extractInviteCode())
    }

    @Test
    fun `a null intent returns null`() {
        val intent: Intent? = null

        assertNull(intent.extractInviteCode())
    }
}

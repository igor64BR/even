package com.rateio.app

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Cobre T22.1: [extractInviteCode] é a única lógica nova de parsing do deep link
 * `rateio://join/{codigo}` (`AndroidManifest.xml`) — precisa de `Intent`/`Uri` de verdade
 * (Robolectric), mesmo runner de `GroupDetailViewModelTest`.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityDeepLinkTest {

    @Test
    fun `intent VIEW com o deep link extrai o codigo do convite`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("rateio://join/ABC123"))

        assertEquals("ABC123", intent.extractInviteCode())
    }

    @Test
    fun `intent de abertura normal pelo launcher nao tem codigo de convite`() {
        val intent = Intent(Intent.ACTION_MAIN)

        assertNull(intent.extractInviteCode())
    }

    @Test
    fun `scheme diferente nao e reconhecido como o deep link do app`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/join/ABC123"))

        assertNull(intent.extractInviteCode())
    }

    @Test
    fun `host diferente de join nao e reconhecido`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("rateio://outracoisa/ABC123"))

        assertNull(intent.extractInviteCode())
    }

    @Test
    fun `link sem codigo no path retorna null`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("rateio://join/"))

        assertNull(intent.extractInviteCode())
    }

    @Test
    fun `intent nulo retorna null`() {
        val intent: Intent? = null

        assertNull(intent.extractInviteCode())
    }
}

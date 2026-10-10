package io.github.salihoguz.minlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchTest {

    private val apps = listOf("Keep Notes", "Kindle", "Google Drive", "e-Devlet", "Gmail", "Maps", "Şok Market", "Hevy")

    private fun search(query: String) = Search.filter(apps, query) { it }

    @Test
    fun normalizeDropsSymbolsAndAccents() {
        assertEquals("edevlet", Search.normalize("e-Devlet"))
        assertEquals("sok market", Search.normalize("Şok Market"))
        assertEquals("istanbul", Search.normalize("İstanbul"))
        assertEquals("kirmizi", Search.normalize("kırmızı"))
    }

    @Test
    fun prefixBeatsWordPrefixBeatsInitialsBeatsContains() {
        assertEquals(0, Search.score("Keep Notes", "kee"))
        assertEquals(1, Search.score("Keep Notes", "not"))
        assertEquals(2, Search.score("Keep Notes", "kn"))
        assertEquals(3, Search.score("Keep Notes", "otes"))
        assertNull(Search.score("Keep Notes", "xyz"))
        assertNull(Search.score("Keep Notes", "  "))
    }

    @Test
    fun ignoresSymbolsWhenTyping() {
        assertEquals(listOf("e-Devlet"), search("edev"))
        assertEquals(listOf("e-Devlet"), search("e-dev"))
        assertEquals(listOf("Şok Market"), search("sok"))
    }

    @Test
    fun ordersByMatchQualityThenName() {
        assertEquals(listOf("Gmail", "Google Drive"), search("g"))
        assertEquals(listOf("Google Drive"), search("gd"))
        assertEquals(listOf("Keep Notes", "Kindle", "Şok Market"), search("k"))
    }
}

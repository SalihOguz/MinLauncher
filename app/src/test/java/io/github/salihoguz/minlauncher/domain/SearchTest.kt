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
        assertEquals(Search.PREFIX, Search.score("Keep Notes", "kee"))
        assertEquals(Search.WORD_PREFIX, Search.score("Keep Notes", "not"))
        assertEquals(Search.INITIALS, Search.score("Keep Notes", "kn"))
        assertEquals(Search.CONTAINS, Search.score("Keep Notes", "otes"))
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
        assertEquals(listOf("Keep Notes", "Kindle"), search("k"))
    }

    @Test
    fun shortQueriesDontMatchInsideNames() {
        assertNull(Search.score("Signal", "al"))
        assertEquals(Search.CONTAINS, Search.score("Signal", "nal"))
        assertEquals(Search.PREFIX, Search.score("Alarmy", "al"))
    }

    @Test
    fun searchWordsFindAppsByWhatTheyDo() {
        assertEquals(Search.SEARCH_WORD, Search.score("Komoot", "hik", listOf("hiking", "bike")))
        assertEquals(Search.SEARCH_WORD, Search.score("Halkbank", "mon", listOf("money")))
        assertNull(Search.score("Komoot", "hik"))
        val apps = listOf("Akbank", "Halkbank", "Hevy")
        val words = mapOf("Halkbank" to listOf("bank", "money"), "Akbank" to listOf("bank"))
        assertEquals(listOf("Akbank", "Halkbank"), Search.filter(apps, "bank", searchWords = { words[it].orEmpty() }) { it })
    }

    @Test
    fun toleratesSmallTyposFromFourLetters() {
        assertEquals(Search.TYPO, Search.score("Alarmy", "alrmy"))
        assertEquals(Search.TYPO, Search.score("Keep Notes", "kepe"))
        assertEquals(Search.TYPO, Search.score("WhatsApp", "whta"))
        assertNull(Search.score("Alarmy", "alr"))
        assertNull(Search.score("Signal", "brave"))
    }

    @Test
    fun editDistanceCountsSwapsAsOne() {
        assertEquals(0, Search.editDistance("abc", "abc"))
        assertEquals(1, Search.editDistance("abc", "acb"))
        assertEquals(1, Search.editDistance("abc", "abcd"))
        assertEquals(2, Search.editDistance("kitten", "sittin"))
    }
}

package io.github.salihoguz.minlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FoldersTest {

    @Test
    fun suggestsFromAndroidCategory() {
        assertEquals("Travel", Folders.suggestion(6, emptyList()))
        assertEquals("Games", Folders.suggestion(0, emptyList()))
        assertEquals("Media", Folders.suggestion(1, emptyList()))
        assertNull(Folders.suggestion(-1, emptyList()))
        assertNull(Folders.suggestion(8, emptyList()))
    }

    @Test
    fun suggestionReusesExistingFolderWhateverItsCapitalisation() {
        assertEquals("travel", Folders.suggestion(6, listOf("Money", "travel")))
    }

    @Test
    fun namesCategories() {
        assertEquals("Maps", Folders.categoryName(6))
        assertNull(Folders.categoryName(-1))
    }

    @Test
    fun validatesFolderNames() {
        val existing = listOf("Money", "Travel")
        assertNull(Folders.problemWithName("Banks", existing))
        assertNotNull(Folders.problemWithName("  ", existing))
        assertNotNull(Folders.problemWithName("money", existing))
        assertNotNull(Folders.problemWithName("unsorted", existing))
        assertNotNull(Folders.problemWithName("x".repeat(25), existing))
        // Renaming a folder to a different capitalisation of itself is fine.
        assertNull(Folders.problemWithName("MONEY", existing, renaming = "Money"))
    }

    @Test
    fun railLetters() {
        assertEquals('A', Folders.railLetter("Alarmy"))
        assertEquals('S', Folders.railLetter("Şok Market"))
        assertEquals('I', Folders.railLetter("İstanbulkart"))
        assertEquals('E', Folders.railLetter("e-Devlet"))
        assertEquals('#', Folders.railLetter("1Password"))
        assertEquals('#', Folders.railLetter(""))
        assertEquals(27, Folders.RAIL_LETTERS.size)
    }
}

package io.github.salihoguz.minlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RailTest {

    @Test
    fun filesNamesUnderTheirFirstLetter() {
        assertEquals('A', Rail.letterOf("Alarmy"))
        assertEquals('S', Rail.letterOf("Şok Market"))
        assertEquals('I', Rail.letterOf("İstanbulkart"))
        assertEquals('E', Rail.letterOf("e-Devlet"))
        assertEquals('#', Rail.letterOf("1Password"))
        assertEquals('#', Rail.letterOf(""))
        assertEquals(27, Rail.LETTERS.size)
        assertEquals('#', Rail.LETTERS.first())
    }

    @Test
    fun jumpsToTheFirstAppOfALetterOrTheNextLetterWithApps() {
        val labels = listOf("1Password", "Alarmy", "Amazon", "Camera", "Maps", "Notion", "Zoom")
        val targets = Rail.targets(labels)
        assertEquals(0, targets['#'])
        assertEquals(1, targets['A'])
        // No B apps: B goes to Camera.
        assertEquals(3, targets['B'])
        assertEquals(4, targets['M'])
        assertEquals(6, targets['O'])
        assertEquals(6, targets['Z'])
    }

    @Test
    fun lettersAfterTheLastAppGoToTheLastApp() {
        val targets = Rail.targets(listOf("Alarmy", "Camera"))
        assertEquals(1, targets['X'])
        assertTrue(Rail.targets(emptyList()).isEmpty())
    }
}

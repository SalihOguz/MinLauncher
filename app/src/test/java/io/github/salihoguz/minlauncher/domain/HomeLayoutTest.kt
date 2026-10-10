package io.github.salihoguz.minlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun oneColumnUpTo14() {
        for (count in 1..14) {
            assertEquals(1, HomeLayout.columns(count))
            assertEquals(count, HomeLayout.rows(count))
        }
    }

    @Test
    fun twoBalancedColumnsFrom15To28() {
        assertEquals(2, HomeLayout.columns(15))
        assertEquals(8, HomeLayout.rows(15))
        assertEquals(9, HomeLayout.rows(18))
        assertEquals(14, HomeLayout.rows(28))
    }

    @Test
    fun autoClockShowsWhileElevenRowsOrFewer() {
        assertTrue(HomeLayout.showClock(ClockMode.AUTO, 0))
        assertTrue(HomeLayout.showClock(ClockMode.AUTO, 11))
        assertFalse(HomeLayout.showClock(ClockMode.AUTO, 12))
        assertFalse(HomeLayout.showClock(ClockMode.AUTO, 14))
        assertTrue(HomeLayout.showClock(ClockMode.AUTO, 15))
        assertTrue(HomeLayout.showClock(ClockMode.AUTO, 22))
        assertFalse(HomeLayout.showClock(ClockMode.AUTO, 23))
        assertFalse(HomeLayout.showClock(ClockMode.AUTO, 28))
    }

    @Test
    fun alwaysAndNeverIgnoreTheCount() {
        assertTrue(HomeLayout.showClock(ClockMode.ALWAYS, 28))
        assertFalse(HomeLayout.showClock(ClockMode.NEVER, 1))
    }

    @Test
    fun columnsFillTopToBottomLeftFirst() {
        // 15 apps: 8 rows, left column holds 0..7, right column holds 8..14.
        assertEquals(0 to 0, HomeLayout.cellOf(0, 15))
        assertEquals(0 to 7, HomeLayout.cellOf(7, 15))
        assertEquals(1 to 0, HomeLayout.cellOf(8, 15))
        assertEquals(1 to 6, HomeLayout.cellOf(14, 15))
        assertEquals(0 to 13, HomeLayout.cellOf(13, 14))
    }

    @Test
    fun indexAtRoundTripsAndClamps() {
        for (count in listOf(1, 9, 14, 15, 21, 28)) {
            for (i in 0 until count) {
                val (column, row) = HomeLayout.cellOf(i, count)
                assertEquals(i, HomeLayout.indexAt(column, row, count))
            }
        }
        // The empty last slot of an odd two-column list clamps to the last app.
        assertEquals(14, HomeLayout.indexAt(1, 7, 15))
        assertEquals(0, HomeLayout.indexAt(-3, -3, 15))
        assertEquals(27, HomeLayout.indexAt(5, 50, 28))
    }
}

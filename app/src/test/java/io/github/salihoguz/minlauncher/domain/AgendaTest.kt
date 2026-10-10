package io.github.salihoguz.minlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgendaTest {

    private val minute = 60_000L
    private val day = Sections.DAY_MS
    // Local midnight today, and today's date as a UTC midnight (the same here: UTC phone).
    private val dayStart = 20_000 * day
    private val dayEnd = dayStart + day
    private val now = dayStart + 10 * 60 * minute

    private fun at(minutes: Int) = dayStart + minutes * minute
    private fun timed(id: Long, from: Int, to: Int) = CalendarEvent(id, "e$id", at(from), at(to), allDay = false)
    private fun allDay(id: Long, days: Int = 1) = CalendarEvent(id, "a$id", dayStart, dayStart + days * day, allDay = true)

    @Test
    fun showsTheNextEventLaterToday() {
        val events = listOf(timed(1, 14 * 60, 15 * 60), timed(2, 12 * 60, 13 * 60), timed(3, 8 * 60, 9 * 60))
        assertEquals(2L, Agenda.next(events, now, dayEnd, dayStart)?.id)
    }

    @Test
    fun showsWhatIsGoingOnUnlessSomethingStartsSoon() {
        val work = timed(1, 9 * 60, 18 * 60)
        val dentist = timed(2, 14 * 60, 15 * 60)
        assertEquals(1L, Agenda.next(listOf(work, dentist), now, dayEnd, dayStart)?.id)
        assertEquals(2L, Agenda.next(listOf(work, dentist), at(13 * 60 + 40), dayEnd, dayStart)?.id)
    }

    @Test
    fun allDayEventsOnlyWhenNothingTimedIsLeft() {
        val events = listOf(allDay(1), timed(2, 16 * 60, 17 * 60))
        assertEquals(2L, Agenda.next(events, now, dayEnd, dayStart)?.id)
        assertEquals(1L, Agenda.next(events, at(18 * 60), dayEnd, dayStart)?.id)
    }

    @Test
    fun aFreeDayShowsNothing() {
        val yesterday = CalendarEvent(1, "old", dayStart - day, dayStart, allDay = true)
        val tomorrow = timed(2, 26 * 60, 27 * 60)
        assertNull(Agenda.next(listOf(yesterday, tomorrow), now, dayEnd, dayStart))
    }
}

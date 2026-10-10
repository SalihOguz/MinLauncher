package io.github.salihoguz.minlauncher.domain

/** One calendar event on one day. All-day events use UTC midnights, as Android stores them. */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
)

object Agenda {
    /** An event starting this soon is shown even while another is going on. */
    const val SOON_MS = 30 * 60 * 1000L

    /**
     * Today's event for the home line, or null on a free day:
     * - the next one, if it starts within 30 minutes or nothing is going on;
     * - otherwise the one going on now (the one ending first, if several);
     * - all-day events only when no timed event is left today.
     *
     * [dayEnd] is the end of today, local time. [todayUtc] is today's date as a UTC midnight,
     * to match all-day events.
     */
    fun next(events: List<CalendarEvent>, now: Long, dayEnd: Long, todayUtc: Long): CalendarEvent? {
        val timed = events.filter { !it.allDay && it.end > now && it.begin < dayEnd }
        val ongoing = timed.filter { it.begin <= now }.minByOrNull { it.end }
        val upcoming = timed.filter { it.begin > now }.minByOrNull { it.begin }
        return when {
            upcoming != null && (ongoing == null || upcoming.begin - now <= SOON_MS) -> upcoming
            ongoing != null -> ongoing
            else -> events
                .filter { it.allDay && it.begin <= todayUtc && it.end > todayUtc }
                .minByOrNull { it.begin }
        }
    }
}

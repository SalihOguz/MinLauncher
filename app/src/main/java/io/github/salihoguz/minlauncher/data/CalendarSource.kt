package io.github.salihoguz.minlauncher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import io.github.salihoguz.minlauncher.domain.Agenda
import io.github.salihoguz.minlauncher.domain.CalendarEvent
import io.github.salihoguz.minlauncher.domain.Sections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

/** One of the phone's calendars, for the picker. */
data class PhoneCalendar(val id: Long, val name: String, val account: String, val color: Int)

/** Reads the phone's calendars, only the ones picked in Settings. Nothing is written. */
class CalendarSource(private val context: Context) {

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun calendars(): List<PhoneCalendar> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        runCatching {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(PhoneCalendar(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getInt(3)))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList()).sortedWith(compareBy<PhoneCalendar>({ it.account }, { it.name }))
    }

    /** Today's event for the home line from the picked calendars, or null. */
    suspend fun nextEvent(calendarIds: Set<Long>, now: Long): CalendarEvent? = withContext(Dispatchers.IO) {
        if (calendarIds.isEmpty() || !hasPermission()) return@withContext null
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayEnd = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        // All-day events are stored on UTC midnights.
        val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(dayStart.get(Calendar.YEAR), dayStart.get(Calendar.MONTH), dayStart.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            // A day either side, so all-day events in any time zone are included.
            ContentUris.appendId(it, dayStart.timeInMillis - Sections.DAY_MS)
            ContentUris.appendId(it, dayEnd + Sections.DAY_MS)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
        )
        val events = runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        if (c.getLong(5) !in calendarIds) continue
                        val title = c.getString(1)?.trim().orEmpty().ifEmpty { "Busy" }
                        add(CalendarEvent(c.getLong(0), title, c.getLong(2), c.getLong(3), allDay = c.getInt(4) == 1))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
        Agenda.next(events, now, dayEnd, todayUtc)
    }
}

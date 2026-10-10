package io.github.salihoguz.minlauncher.data

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import io.github.salihoguz.minlauncher.domain.DayUsage
import io.github.salihoguz.minlauncher.domain.Stats
import io.github.salihoguz.minlauncher.domain.UsageEvent
import java.io.File
import java.util.Calendar

/**
 * Reads Android's own app usage records and app sizes. Most of it needs "Usage access",
 * which the user turns on in Android's settings; without it everything here returns empty
 * and the app falls back to what MinLauncher records itself.
 */
class Usage(private val context: Context) {

    private val usageStats = context.getSystemService(UsageStatsManager::class.java)

    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** When each package was last used, over the past year. */
    fun lastUsed(now: Long): Map<String, Long> = aggregate(now - YEAR_MS, now)
        .mapValues { it.value.lastTimeUsed }
        .filterValues { it > 0 }

    /** Time each package spent on screen since [since]. */
    fun foregroundTime(since: Long, now: Long): Map<String, Long> = aggregate(since, now)
        .mapValues { it.value.totalTimeInForeground }
        .filterValues { it > 0 }

    /** Today's screen time and unlocks. MinLauncher itself doesn't count as screen time. */
    fun today(now: Long): DayUsage? {
        val dayStart = startOfDay(now)
        val events = runCatching { usageStats.queryEvents(dayStart, now) }.getOrNull() ?: return null
        val list = ArrayList<UsageEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = when (event.eventType) {
                EVENT_RESUMED -> UsageEvent.Type.RESUMED
                EVENT_PAUSED -> UsageEvent.Type.PAUSED
                EVENT_SCREEN_OFF -> UsageEvent.Type.SCREEN_OFF
                EVENT_UNLOCKED -> UsageEvent.Type.UNLOCKED
                else -> continue
            }
            val pkg = event.packageName ?: continue
            list += UsageEvent(event.timeStamp, type, pkg, "$pkg/${event.className}")
        }
        return Stats.day(list, dayStart, now, ignore = setOf(context.packageName))
    }

    /**
     * The app's size. With usage access: the app plus its data and cache, as Android's
     * storage settings show it. Without: just the installed app files.
     */
    fun size(app: InstalledApp, access: Boolean): Long? {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val info = runCatching { launcherApps.getApplicationInfo(app.packageName, 0, app.user) }.getOrNull() ?: return null
        if (access) {
            runCatching {
                val stats = context.getSystemService(StorageStatsManager::class.java)
                    .queryStatsForPackage(info.storageUuid, app.packageName, app.user)
                return stats.appBytes + stats.dataBytes
            }
        }
        return runCatching {
            (listOf(info.sourceDir) + info.splitSourceDirs.orEmpty()).sumOf { File(it).length() }
        }.getOrNull()?.takeIf { it > 0 }
    }

    private fun aggregate(begin: Long, end: Long) =
        runCatching { usageStats.queryAndAggregateUsageStats(begin, end) }.getOrNull().orEmpty()

    companion object {
        private const val YEAR_MS = 365L * 24 * 60 * 60 * 1000

        // UsageEvents.Event types, by number: the names changed in Android 10.
        private const val EVENT_RESUMED = 1
        private const val EVENT_PAUSED = 2
        private const val EVENT_SCREEN_OFF = 16
        private const val EVENT_UNLOCKED = 18

        fun startOfDay(now: Long): Long = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

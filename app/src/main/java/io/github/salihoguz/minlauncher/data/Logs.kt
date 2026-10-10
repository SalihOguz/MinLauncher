package io.github.salihoguz.minlauncher.data

import android.content.Context
import io.github.salihoguz.minlauncher.domain.Launch
import io.github.salihoguz.minlauncher.domain.LaunchSource
import io.github.salihoguz.minlauncher.domain.Sections
import io.github.salihoguz.minlauncher.domain.Visit
import io.github.salihoguz.minlauncher.domain.VisitOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A small CSV on the phone, one line per event, starting with the time. Kept for 60 days.
 * Nothing leaves the device.
 */
private class CsvLog(context: Context, name: String) {

    private val file = File(context.filesDir, name)
    private val lock = Mutex()

    suspend fun append(vararg fields: Any) = withContext(Dispatchers.IO) {
        lock.withLock {
            runCatching {
                file.appendText(fields.joinToString(separator = ",", postfix = "\n"))
                if (file.length() > MAX_BYTES) trim()
            }
        }
    }

    suspend fun read(): List<List<String>> = withContext(Dispatchers.IO) {
        lock.withLock {
            runCatching { file.readLines().filter { it.isNotBlank() }.map { it.split(',') } }.getOrDefault(emptyList())
        }
    }

    private fun trim() {
        val cutoff = System.currentTimeMillis() - KEEP_DAYS * Sections.DAY_MS
        val kept = file.readLines().filter { line -> (line.substringBefore(',').toLongOrNull() ?: 0L) >= cutoff }
        file.writeText(kept.joinToString(separator = "\n", postfix = if (kept.isEmpty()) "" else "\n"))
    }

    private companion object {
        const val MAX_BYTES = 256 * 1024
        const val KEEP_DAYS = 60
    }
}

/** How each drawer visit ended: time, outcome, whether the keyboard was put away first. */
class VisitLog(context: Context) {

    private val log = CsvLog(context, "drawer-visits.csv")

    suspend fun record(outcome: VisitOutcome, keyboardClosedFirst: Boolean) =
        log.append(System.currentTimeMillis(), outcome.name, if (keyboardClosedFirst) 1 else 0)

    suspend fun read(): List<Visit> = log.read().mapNotNull { fields ->
        val time = fields.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val outcome = VisitOutcome.entries.firstOrNull { it.name == fields.getOrNull(1) } ?: return@mapNotNull null
        Visit(time, outcome, keyboardClosedFirst = fields.getOrNull(2) == "1")
    }
}

/** Every app opened from MinLauncher: time, where from, app key. */
class LaunchLog(context: Context) {

    private val log = CsvLog(context, "launches.csv")

    suspend fun record(key: String, source: LaunchSource) = log.append(System.currentTimeMillis(), source.name, key)

    suspend fun read(): List<Launch> = log.read().mapNotNull { fields ->
        val time = fields.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val source = LaunchSource.entries.firstOrNull { it.name == fields.getOrNull(1) } ?: return@mapNotNull null
        // Keys never contain commas, but join the rest anyway to be safe.
        val key = fields.drop(2).joinToString(",").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        Launch(time, key, source)
    }
}

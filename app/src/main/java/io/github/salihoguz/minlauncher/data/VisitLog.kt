package io.github.salihoguz.minlauncher.data

import android.content.Context
import io.github.salihoguz.minlauncher.domain.Sections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** How a drawer visit ended. Feeds the Stats tab (v0.4). */
enum class VisitOutcome { PINNED, RECENT, NEW, LIST, SEARCH, WEB, FOLDER, RAIL, NONE }

/**
 * A small CSV on the phone: one line per drawer visit, kept for 60 days.
 * Nothing leaves the device.
 */
class VisitLog(context: Context) {

    private val file = File(context.filesDir, "drawer-visits.csv")

    suspend fun record(outcome: VisitOutcome, keyboardClosedFirst: Boolean) = withContext(Dispatchers.IO) {
        runCatching {
            file.appendText("${System.currentTimeMillis()},${outcome.name},${if (keyboardClosedFirst) 1 else 0}\n")
            if (file.length() > MAX_BYTES) trim()
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

package io.github.salihoguz.minlauncher.domain

enum class ClockMode { AUTO, ALWAYS, NEVER }

/**
 * Where each home app sits. Up to 14 apps use one column; 15–28 use two balanced
 * columns, filled top to bottom, left column first.
 */
object HomeLayout {
    const val MAX_APPS = 28
    const val ONE_COLUMN_MAX = 14
    /** The big clock fits while the list has this many rows or fewer. */
    const val CLOCK_MAX_ROWS = 11

    fun columns(count: Int): Int = if (count > ONE_COLUMN_MAX) 2 else 1

    fun rows(count: Int): Int {
        if (count <= 0) return 0
        val cols = columns(count)
        return (count + cols - 1) / cols
    }

    fun showClock(mode: ClockMode, count: Int): Boolean = when (mode) {
        ClockMode.ALWAYS -> true
        ClockMode.NEVER -> false
        ClockMode.AUTO -> rows(count) <= CLOCK_MAX_ROWS
    }

    /** Column and row of the app at [index]. */
    fun cellOf(index: Int, count: Int): Pair<Int, Int> {
        val rows = rows(count)
        if (rows == 0) return 0 to 0
        return (index / rows) to (index % rows)
    }

    /** Index of the app at [column] and [row], clamped to the apps that exist. */
    fun indexAt(column: Int, row: Int, count: Int): Int {
        if (count <= 0) return 0
        val rows = rows(count)
        val col = column.coerceIn(0, columns(count) - 1)
        val r = row.coerceIn(0, rows - 1)
        return (col * rows + r).coerceIn(0, count - 1)
    }
}

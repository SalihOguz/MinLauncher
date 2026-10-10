package io.github.salihoguz.minlauncher.domain

/** The A–Z rail: jumps through the alphabetical app list. */
object Rail {
    /** # first: names starting with a digit or symbol sort before A. */
    val LETTERS: List<Char> = listOf('#') + ('A'..'Z')

    /** The letter an app is filed under: A–Z, or # for anything else. */
    fun letterOf(label: String): Char {
        val first = Search.normalize(label).firstOrNull()?.uppercaseChar() ?: return '#'
        return if (first in 'A'..'Z') first else '#'
    }

    /**
     * For every rail letter, the position in [labels] (already in alphabetical order) to jump to:
     * the first app under that letter, or, for letters without apps, the first app of the next
     * letter that has some (the last app when none follows). Empty when there are no apps.
     */
    fun targets(labels: List<String>): Map<Char, Int> {
        if (labels.isEmpty()) return emptyMap()
        val first = HashMap<Char, Int>()
        labels.forEachIndexed { index, label -> first.putIfAbsent(letterOf(label), index) }
        val result = HashMap<Char, Int>()
        var next = labels.lastIndex
        for (letter in LETTERS.asReversed()) {
            first[letter]?.let { next = it }
            result[letter] = next
        }
        return result
    }
}

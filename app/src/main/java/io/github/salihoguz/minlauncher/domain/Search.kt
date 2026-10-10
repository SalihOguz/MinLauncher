package io.github.salihoguz.minlauncher.domain

import java.text.Normalizer
import java.util.Locale

/** Matches app names against what the user types. Lower score = better match. */
object Search {

    private val combiningMarks = Regex("\\p{Mn}+")
    private val wordSplit = Regex("[\\s._\\-/:&+]+")
    private val nonAlphanumeric = Regex("[^a-z0-9 ]")

    /** Lowercase, no accents (ş → s, ı → i), no symbols. "e-Devlet" → "edevlet". */
    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT).replace('ı', 'i')
        val stripped = combiningMarks.replace(Normalizer.normalize(lower, Normalizer.Form.NFD), "")
        return nonAlphanumeric.replace(stripped, "").trim()
    }

    fun score(label: String, query: String): Int? {
        val q = normalize(query)
        if (q.isEmpty()) return null
        val compactQuery = q.replace(" ", "")
        val words = label.split(wordSplit).map(::normalize).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val compact = words.joinToString("")
        val spaced = words.joinToString(" ")
        return when {
            spaced.startsWith(q) || compact.startsWith(compactQuery) -> 0
            words.any { it.startsWith(compactQuery) } -> 1
            words.size > 1 && compactQuery.length >= 2 &&
                words.joinToString("") { it.take(1) }.startsWith(compactQuery) -> 2
            // Matches inside a name only from 3 letters on; "al" finding "Signal" is noise.
            compactQuery.length >= 3 && compact.contains(compactQuery) -> 3
            else -> null
        }
    }

    /** Matching items, best first, then alphabetical. */
    fun <T> filter(items: List<T>, query: String, label: (T) -> String): List<T> =
        items.mapNotNull { item -> score(label(item), query)?.let { item to it } }
            .sortedWith(compareBy<Pair<T, Int>> { it.second }.thenBy { normalize(label(it.first)) })
            .map { it.first }
}

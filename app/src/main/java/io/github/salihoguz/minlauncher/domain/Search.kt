package io.github.salihoguz.minlauncher.domain

import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

/** Matches app names against what the user types. Lower score = better match. */
object Search {

    const val PREFIX = 0
    const val WORD_PREFIX = 1
    const val SEARCH_WORD = 2
    const val INITIALS = 3
    const val CONTAINS = 4
    const val TYPO = 5

    private val combiningMarks = Regex("\\p{Mn}+")
    private val wordSplit = Regex("[\\s._\\-/:&+]+")
    private val nonAlphanumeric = Regex("[^a-z0-9 ]")

    /** Lowercase, no accents (ş → s, ı → i), no symbols. "e-Devlet" → "edevlet". */
    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT).replace('ı', 'i')
        val stripped = combiningMarks.replace(Normalizer.normalize(lower, Normalizer.Form.NFD), "")
        return nonAlphanumeric.replace(stripped, "").trim()
    }

    /**
     * How well [label] (and the user's [searchWords] for it) matches [query], or null.
     * Matching inside a name needs 3 letters, typo tolerance needs 4, so short queries stay precise.
     */
    fun score(label: String, query: String, searchWords: List<String> = emptyList()): Int? {
        val q = normalize(query)
        if (q.isEmpty()) return null
        val compactQuery = q.replace(" ", "")
        val words = label.split(wordSplit).map(::normalize).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val compact = words.joinToString("")
        val spaced = words.joinToString(" ")
        val extra = searchWords.map(::normalize).filter { it.isNotEmpty() }
        return when {
            spaced.startsWith(q) || compact.startsWith(compactQuery) -> PREFIX
            words.any { it.startsWith(compactQuery) } -> WORD_PREFIX
            extra.any { it.startsWith(q) || it.replace(" ", "").startsWith(compactQuery) } -> SEARCH_WORD
            words.size > 1 && compactQuery.length >= 2 &&
                words.joinToString("") { it.take(1) }.startsWith(compactQuery) -> INITIALS
            compactQuery.length >= 3 && compact.contains(compactQuery) -> CONTAINS
            compactQuery.length >= 4 && (words + extra).any { isTypoOfPrefix(compactQuery, it) } -> TYPO
            else -> null
        }
    }

    /** Matching items, best first, then alphabetical. */
    fun <T> filter(
        items: List<T>,
        query: String,
        searchWords: (T) -> List<String> = { emptyList() },
        label: (T) -> String,
    ): List<T> =
        items.mapNotNull { item -> score(label(item), query, searchWords(item))?.let { item to it } }
            .sortedWith(compareBy<Pair<T, Int>> { it.second }.thenBy { normalize(label(it.first)) })
            .map { it.first }

    /** True when [query] is one edit (two from 7 letters) away from how [word] starts. */
    internal fun isTypoOfPrefix(query: String, word: String): Boolean {
        val allowed = if (query.length >= 7) 2 else 1
        if (word.length < query.length - allowed) return false
        return (query.length - 1..query.length + 1).any { len ->
            len in 1..word.length && editDistance(query, word.take(len)) <= allowed
        }
    }

    /** Optimal string alignment distance: insertions, deletions, substitutions and swaps. */
    internal fun editDistance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var best = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = min(best, d[i - 2][j - 2] + 1)
                }
                d[i][j] = best
            }
        }
        return d[a.length][b.length]
    }
}

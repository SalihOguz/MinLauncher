package io.github.salihoguz.minlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.salihoguz.minlauncher.domain.ClockMode
import io.github.salihoguz.minlauncher.domain.Folders
import io.github.salihoguz.minlauncher.domain.HomeLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher")

data class LauncherSettings(
    val home: List<String> = emptyList(),
    val pinned: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val renames: Map<String, String> = emptyMap(),
    val searchWords: Map<String, List<String>> = emptyMap(),
    val clockMode: ClockMode = ClockMode.AUTO,
    val keyboardOnOpen: Boolean = true,
    val openOnlyMatch: Boolean = false,
    val enterSearchesWeb: Boolean = true,
    val doubleTapToLock: Boolean = true,
    /** How much the drawer darkens the wallpaper, 0..1. */
    val drawerDim: Float = 0.45f,
    /** How many Recent apps the drawer shows: 0, 5 or 10. */
    val recentCount: Int = 5,
    /** The A–Z rail down the drawer's right edge. */
    val alphabetRail: Boolean = true,
    /** When MinLauncher started keeping track. Apps installed after this can be New. */
    val trackingSince: Long = Long.MAX_VALUE,
    /** Last time each app was opened from MinLauncher. */
    val lastOpened: Map<String, Long> = emptyMap(),
    /** Per-app "Show in Recent" choices; apps without one follow the default (off for games). */
    val showInRecent: Map<String, Boolean> = emptyMap(),
    /** When each app was removed from Recent. */
    val removedFromRecent: Map<String, Long> = emptyMap(),
    val dismissedFromNew: Set<String> = emptySet(),
    /** User folders, in the order they show. */
    val folders: List<String> = emptyList(),
    /** Which folder each app is in. Apps without one are in Unsorted. */
    val folderOf: Map<String, String> = emptyMap(),
    /** When each app was put on home or pinned (recorded from v0.4 on). */
    val placedAt: Map<String, Long> = emptyMap(),
    /** When "Not now" was pressed on each Stats suggestion. */
    val snoozedSuggestions: Map<String, Long> = emptyMap(),
)

class SettingsStore(context: Context) {

    private val store = context.dataStore

    val settings: Flow<LauncherSettings> = store.data.map { p ->
        val renames = HashMap<String, String>()
        val words = HashMap<String, List<String>>()
        val lastOpened = HashMap<String, Long>()
        val showInRecent = HashMap<String, Boolean>()
        val removed = HashMap<String, Long>()
        val folderOf = HashMap<String, String>()
        val placedAt = HashMap<String, Long>()
        val snoozed = HashMap<String, Long>()
        for ((key, value) in p.asMap()) {
            val name = key.name
            when {
                name.startsWith(RENAME) -> renames[name.removePrefix(RENAME)] = value.toString()
                name.startsWith(WORDS) -> words[name.removePrefix(WORDS)] = splitWords(value.toString())
                name.startsWith(LAST_OPENED) -> (value as? Long)?.let { lastOpened[name.removePrefix(LAST_OPENED)] = it }
                name.startsWith(SHOW_IN_RECENT) -> (value as? Boolean)?.let { showInRecent[name.removePrefix(SHOW_IN_RECENT)] = it }
                name.startsWith(REMOVED_FROM_RECENT) -> (value as? Long)?.let { removed[name.removePrefix(REMOVED_FROM_RECENT)] = it }
                name.startsWith(FOLDER) -> folderOf[name.removePrefix(FOLDER)] = value.toString()
                name.startsWith(PLACED) -> (value as? Long)?.let { placedAt[name.removePrefix(PLACED)] = it }
                name.startsWith(SNOOZED) -> (value as? Long)?.let { snoozed[name.removePrefix(SNOOZED)] = it }
            }
        }
        LauncherSettings(
            home = p[HOME].toKeyList(),
            pinned = p[PINNED].toKeyList(),
            hidden = p[HIDDEN].orEmpty(),
            renames = renames,
            searchWords = words,
            clockMode = p[CLOCK_MODE]?.let { name -> ClockMode.entries.firstOrNull { it.name == name } } ?: ClockMode.AUTO,
            keyboardOnOpen = p[KEYBOARD_ON_OPEN] ?: true,
            openOnlyMatch = p[OPEN_ONLY_MATCH] ?: false,
            enterSearchesWeb = p[ENTER_SEARCHES_WEB] ?: true,
            doubleTapToLock = p[DOUBLE_TAP_LOCK] ?: true,
            drawerDim = p[DRAWER_DIM] ?: 0.45f,
            recentCount = p[RECENT_COUNT] ?: 5,
            alphabetRail = p[ALPHABET_RAIL] ?: true,
            trackingSince = p[TRACKING_SINCE] ?: Long.MAX_VALUE,
            lastOpened = lastOpened,
            showInRecent = showInRecent,
            removedFromRecent = removed,
            dismissedFromNew = p[DISMISSED_FROM_NEW].orEmpty(),
            folders = p[FOLDERS].toKeyList(),
            folderOf = folderOf,
            placedAt = placedAt,
            snoozedSuggestions = snoozed,
        )
    }

    /** Called once on start; from then on, newly installed apps can show up in New. */
    suspend fun ensureTrackingStarted() = store.edit { p ->
        if (p[TRACKING_SINCE] == null) p[TRACKING_SINCE] = System.currentTimeMillis()
    }

    suspend fun recordLaunch(key: String) = store.edit { it[longPreferencesKey(LAST_OPENED + key)] = System.currentTimeMillis() }

    suspend fun setHome(keys: List<String>) = store.edit { it[HOME] = keys.take(HomeLayout.MAX_APPS).joinToString(SEPARATOR) }

    suspend fun setPinned(keys: List<String>) = store.edit { it[PINNED] = keys.joinToString(SEPARATOR) }

    suspend fun addToHome(key: String) = store.edit { p ->
        val home = p[HOME].toKeyList()
        if (key !in home && home.size < HomeLayout.MAX_APPS) {
            p[HOME] = (home + key).joinToString(SEPARATOR)
            p[PINNED] = (p[PINNED].toKeyList() - key).joinToString(SEPARATOR)
            p[longPreferencesKey(PLACED + key)] = System.currentTimeMillis()
        }
    }

    suspend fun removeFromHome(key: String) = store.edit { p ->
        p[HOME] = (p[HOME].toKeyList() - key).joinToString(SEPARATOR)
    }

    suspend fun pin(key: String) = store.edit { p ->
        val pinned = p[PINNED].toKeyList()
        if (key !in pinned) {
            p[PINNED] = (pinned + key).joinToString(SEPARATOR)
            p[longPreferencesKey(PLACED + key)] = System.currentTimeMillis()
        }
    }

    /** Hides a Stats suggestion for 30 days. */
    suspend fun snoozeSuggestion(id: String) = store.edit { it[longPreferencesKey(SNOOZED + id)] = System.currentTimeMillis() }

    /** Every setting and per-app choice, as JSON, for a backup file. */
    suspend fun exportJson(): String {
        val prefs = JSONObject()
        for ((key, value) in store.data.first().asMap()) {
            val entry = when (value) {
                is Boolean -> JSONObject().put("type", "boolean").put("value", value)
                is Int -> JSONObject().put("type", "int").put("value", value)
                is Long -> JSONObject().put("type", "long").put("value", value)
                is Float -> JSONObject().put("type", "float").put("value", value.toDouble())
                is String -> JSONObject().put("type", "string").put("value", value)
                is Set<*> -> JSONObject().put("type", "strings").put("value", JSONArray(value.map { it.toString() }))
                else -> continue
            }
            prefs.put(key.name, entry)
        }
        return JSONObject()
            .put("app", BACKUP_APP)
            .put("format", BACKUP_FORMAT)
            .put("settings", prefs)
            .toString(2)
    }

    /** Replaces everything with a backup made by [exportJson]. False if the file isn't one. */
    suspend fun importJson(text: String): Boolean {
        val entries = runCatching {
            val root = JSONObject(text)
            require(root.optString("app") == BACKUP_APP)
            val prefs = root.getJSONObject("settings")
            prefs.keys().asSequence().mapNotNull { name ->
                val entry = prefs.getJSONObject(name)
                when (entry.getString("type")) {
                    "boolean" -> booleanPreferencesKey(name) to entry.getBoolean("value")
                    "int" -> intPreferencesKey(name) to entry.getInt("value")
                    "long" -> longPreferencesKey(name) to entry.getLong("value")
                    "float" -> floatPreferencesKey(name) to entry.getDouble("value").toFloat()
                    "string" -> stringPreferencesKey(name) to entry.getString("value")
                    "strings" -> {
                        val array = entry.getJSONArray("value")
                        stringSetPreferencesKey(name) to (0 until array.length()).map { array.getString(it) }.toSet()
                    }
                    else -> null
                }
            }.toList()
        }.getOrNull() ?: return false
        // `key to value` here is DataStore's own Preferences.Pair.
        store.edit { p ->
            p.clear()
            p.putAll(*entries.toTypedArray())
        }
        return true
    }

    suspend fun unpin(key: String) = store.edit { p ->
        p[PINNED] = (p[PINNED].toKeyList() - key).joinToString(SEPARATOR)
    }

    suspend fun hide(key: String) = store.edit { p ->
        p[HIDDEN] = p[HIDDEN].orEmpty() + key
        p[HOME] = (p[HOME].toKeyList() - key).joinToString(SEPARATOR)
        p[PINNED] = (p[PINNED].toKeyList() - key).joinToString(SEPARATOR)
    }

    suspend fun unhide(key: String) = store.edit { p -> p[HIDDEN] = p[HIDDEN].orEmpty() - key }

    suspend fun rename(key: String, label: String?, words: List<String>) = store.edit { p ->
        val labelKey = stringPreferencesKey(RENAME + key)
        if (label.isNullOrBlank()) p.remove(labelKey) else p[labelKey] = label.trim()
        val wordsKey = stringPreferencesKey(WORDS + key)
        val cleaned = words.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isEmpty()) p.remove(wordsKey) else p[wordsKey] = cleaned.joinToString(", ")
    }

    suspend fun setShowInRecent(key: String, value: Boolean) = store.edit { it[booleanPreferencesKey(SHOW_IN_RECENT + key)] = value }

    suspend fun removeFromRecent(key: String) = store.edit {
        it[longPreferencesKey(REMOVED_FROM_RECENT + key)] = System.currentTimeMillis()
    }

    suspend fun dismissFromNew(key: String) = store.edit { p ->
        p[DISMISSED_FROM_NEW] = p[DISMISSED_FROM_NEW].orEmpty() + key
    }

    suspend fun createFolder(name: String) = store.edit { p ->
        val folders = p[FOLDERS].toKeyList()
        if (Folders.problemWithName(name, folders) == null) p[FOLDERS] = (folders + name.trim()).joinToString(SEPARATOR)
    }

    /** Files the app in [folder], creating the folder if needed; null puts it back in Unsorted. */
    suspend fun moveToFolder(key: String, folder: String?) = store.edit { p ->
        val prefKey = stringPreferencesKey(FOLDER + key)
        if (folder == null) {
            p.remove(prefKey)
            return@edit
        }
        val folders = p[FOLDERS].toKeyList()
        val existing = folders.firstOrNull { it.equals(folder.trim(), ignoreCase = true) }
        val name = when {
            existing != null -> existing
            Folders.problemWithName(folder, folders) == null -> folder.trim().also { p[FOLDERS] = (folders + it).joinToString(SEPARATOR) }
            else -> return@edit
        }
        p[prefKey] = name
    }

    suspend fun renameFolder(old: String, new: String) = store.edit { p ->
        val folders = p[FOLDERS].toKeyList()
        if (old !in folders || Folders.problemWithName(new, folders, renaming = old) != null) return@edit
        val name = new.trim()
        p[FOLDERS] = folders.map { if (it == old) name else it }.joinToString(SEPARATOR)
        assignmentsTo(p, old).forEach { p[it] = name }
    }

    /** Removes the folder; its apps go back to Unsorted. */
    suspend fun deleteFolder(name: String) = store.edit { p ->
        p[FOLDERS] = (p[FOLDERS].toKeyList() - name).joinToString(SEPARATOR)
        assignmentsTo(p, name).forEach { p.remove(it) }
    }

    suspend fun moveFolder(name: String, by: Int) = store.edit { p ->
        val folders = p[FOLDERS].toKeyList().toMutableList()
        val from = folders.indexOf(name)
        val to = (from + by).coerceIn(0, folders.lastIndex)
        if (from < 0 || from == to) return@edit
        folders.add(to, folders.removeAt(from))
        p[FOLDERS] = folders.joinToString(SEPARATOR)
    }

    private fun assignmentsTo(p: Preferences, folder: String): List<Preferences.Key<String>> =
        p.asMap().entries
            .filter { it.key.name.startsWith(FOLDER) && it.value == folder }
            .map { stringPreferencesKey(it.key.name) }

    suspend fun setClockMode(mode: ClockMode) = store.edit { it[CLOCK_MODE] = mode.name }
    suspend fun setKeyboardOnOpen(value: Boolean) = store.edit { it[KEYBOARD_ON_OPEN] = value }
    suspend fun setOpenOnlyMatch(value: Boolean) = store.edit { it[OPEN_ONLY_MATCH] = value }
    suspend fun setEnterSearchesWeb(value: Boolean) = store.edit { it[ENTER_SEARCHES_WEB] = value }
    suspend fun setDoubleTapToLock(value: Boolean) = store.edit { it[DOUBLE_TAP_LOCK] = value }
    suspend fun setDrawerDim(value: Float) = store.edit { it[DRAWER_DIM] = value.coerceIn(0f, 0.8f) }
    suspend fun setRecentCount(value: Int) = store.edit { it[RECENT_COUNT] = value.coerceIn(0, 10) }
    suspend fun setAlphabetRail(value: Boolean) = store.edit { it[ALPHABET_RAIL] = value }

    private fun String?.toKeyList(): List<String> =
        this?.split(SEPARATOR)?.filter { it.isNotBlank() }?.distinct().orEmpty()

    private fun splitWords(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    private companion object {
        const val SEPARATOR = "\n"
        const val RENAME = "rename:"
        const val WORDS = "words:"
        const val LAST_OPENED = "last:"
        const val SHOW_IN_RECENT = "show_recent:"
        const val REMOVED_FROM_RECENT = "recent_removed:"
        const val FOLDER = "folder:"
        const val PLACED = "placed:"
        const val SNOOZED = "snoozed:"
        const val BACKUP_APP = "MinLauncher"
        const val BACKUP_FORMAT = 1
        val FOLDERS = stringPreferencesKey("folders")
        val HOME = stringPreferencesKey("home")
        val PINNED = stringPreferencesKey("pinned")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val DISMISSED_FROM_NEW = stringSetPreferencesKey("new_dismissed")
        val CLOCK_MODE = stringPreferencesKey("clock_mode")
        val KEYBOARD_ON_OPEN = booleanPreferencesKey("keyboard_on_open")
        val OPEN_ONLY_MATCH = booleanPreferencesKey("open_only_match")
        val ENTER_SEARCHES_WEB = booleanPreferencesKey("enter_searches_web")
        val DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
        val DRAWER_DIM = floatPreferencesKey("drawer_dim")
        val RECENT_COUNT = intPreferencesKey("recent_count")
        val ALPHABET_RAIL = booleanPreferencesKey("alphabet_rail")
        val TRACKING_SINCE = longPreferencesKey("tracking_since")
    }
}

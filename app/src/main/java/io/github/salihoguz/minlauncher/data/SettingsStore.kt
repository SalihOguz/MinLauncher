package io.github.salihoguz.minlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.salihoguz.minlauncher.domain.ClockMode
import io.github.salihoguz.minlauncher.domain.HomeLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher")

data class LauncherSettings(
    val home: List<String> = emptyList(),
    val pinned: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val renames: Map<String, String> = emptyMap(),
    val clockMode: ClockMode = ClockMode.AUTO,
    val keyboardOnOpen: Boolean = true,
    val openOnlyMatch: Boolean = false,
    val enterSearchesWeb: Boolean = true,
    val doubleTapToLock: Boolean = true,
)

class SettingsStore(context: Context) {

    private val store = context.dataStore

    val settings: Flow<LauncherSettings> = store.data.map { p ->
        LauncherSettings(
            home = p[HOME].toKeyList(),
            pinned = p[PINNED].toKeyList(),
            hidden = p[HIDDEN].orEmpty(),
            renames = p.asMap().entries
                .filter { it.key.name.startsWith(RENAME_PREFIX) }
                .associate { it.key.name.removePrefix(RENAME_PREFIX) to it.value.toString() },
            clockMode = p[CLOCK_MODE]?.let { name -> ClockMode.entries.firstOrNull { it.name == name } } ?: ClockMode.AUTO,
            keyboardOnOpen = p[KEYBOARD_ON_OPEN] ?: true,
            openOnlyMatch = p[OPEN_ONLY_MATCH] ?: false,
            enterSearchesWeb = p[ENTER_SEARCHES_WEB] ?: true,
            doubleTapToLock = p[DOUBLE_TAP_LOCK] ?: true,
        )
    }

    suspend fun setHome(keys: List<String>) = store.edit { it[HOME] = keys.take(HomeLayout.MAX_APPS).joinToString(SEPARATOR) }

    suspend fun setPinned(keys: List<String>) = store.edit { it[PINNED] = keys.joinToString(SEPARATOR) }

    suspend fun addToHome(key: String) = store.edit { p ->
        val home = p[HOME].toKeyList()
        if (key !in home && home.size < HomeLayout.MAX_APPS) {
            p[HOME] = (home + key).joinToString(SEPARATOR)
            p[PINNED] = (p[PINNED].toKeyList() - key).joinToString(SEPARATOR)
        }
    }

    suspend fun removeFromHome(key: String) = store.edit { p ->
        p[HOME] = (p[HOME].toKeyList() - key).joinToString(SEPARATOR)
    }

    suspend fun pin(key: String) = store.edit { p ->
        val pinned = p[PINNED].toKeyList()
        if (key !in pinned) p[PINNED] = (pinned + key).joinToString(SEPARATOR)
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

    suspend fun rename(key: String, label: String?) = store.edit { p ->
        val prefKey = stringPreferencesKey(RENAME_PREFIX + key)
        if (label.isNullOrBlank()) p.remove(prefKey) else p[prefKey] = label.trim()
    }

    suspend fun setClockMode(mode: ClockMode) = store.edit { it[CLOCK_MODE] = mode.name }
    suspend fun setKeyboardOnOpen(value: Boolean) = store.edit { it[KEYBOARD_ON_OPEN] = value }
    suspend fun setOpenOnlyMatch(value: Boolean) = store.edit { it[OPEN_ONLY_MATCH] = value }
    suspend fun setEnterSearchesWeb(value: Boolean) = store.edit { it[ENTER_SEARCHES_WEB] = value }
    suspend fun setDoubleTapToLock(value: Boolean) = store.edit { it[DOUBLE_TAP_LOCK] = value }

    private fun String?.toKeyList(): List<String> =
        this?.split(SEPARATOR)?.filter { it.isNotBlank() }?.distinct().orEmpty()

    private companion object {
        const val SEPARATOR = "\n"
        const val RENAME_PREFIX = "rename:"
        val HOME = stringPreferencesKey("home")
        val PINNED = stringPreferencesKey("pinned")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val CLOCK_MODE = stringPreferencesKey("clock_mode")
        val KEYBOARD_ON_OPEN = booleanPreferencesKey("keyboard_on_open")
        val OPEN_ONLY_MATCH = booleanPreferencesKey("open_only_match")
        val ENTER_SEARCHES_WEB = booleanPreferencesKey("enter_searches_web")
        val DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
    }
}

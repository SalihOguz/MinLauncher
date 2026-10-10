package io.github.salihoguz.minlauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.salihoguz.minlauncher.data.AppRepository
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.data.LauncherSettings
import io.github.salihoguz.minlauncher.data.SettingsStore
import io.github.salihoguz.minlauncher.domain.ClockMode
import io.github.salihoguz.minlauncher.domain.HomeLayout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator

data class UiState(
    val loaded: Boolean = false,
    /** Every visible app, alphabetical. */
    val apps: List<LauncherApp> = emptyList(),
    val home: List<LauncherApp> = emptyList(),
    /** Pinned apps that aren't already on home. */
    val pinned: List<LauncherApp> = emptyList(),
    val hidden: List<LauncherApp> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
) {
    val homeKeys: Set<String> get() = home.mapTo(HashSet()) { it.key }
    val pinnedKeys: Set<String> get() = pinned.mapTo(HashSet()) { it.key }
    val homeIsFull: Boolean get() = home.size >= HomeLayout.MAX_APPS
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application, viewModelScope)
    private val store = SettingsStore(application)
    private val collator = Collator.getInstance()

    val state: StateFlow<UiState> = combine(repository.apps, store.settings) { installed, settings ->
        if (installed == null) return@combine UiState(settings = settings)
        val all = installed.map { LauncherApp(it, settings.renames[it.key] ?: it.label) }
        val byKey = all.associateBy { it.key }
        val visible = all.filter { it.key !in settings.hidden }.sortedWith { a, b -> collator.compare(a.label, b.label) }
        val home = settings.home.mapNotNull { byKey[it] }.filter { it.key !in settings.hidden }
        val homeKeys = home.mapTo(HashSet()) { it.key }
        UiState(
            loaded = true,
            apps = visible,
            home = home,
            pinned = settings.pinned.mapNotNull { byKey[it] }.filter { it.key !in settings.hidden && it.key !in homeKeys },
            hidden = all.filter { it.key in settings.hidden }.sortedWith { a, b -> collator.compare(a.label, b.label) },
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun start() = repository.start()
    fun stop() = repository.stop()

    fun reorderHome(keys: List<String>) = save { store.setHome(keys) }
    fun reorderPinned(keys: List<String>) = save {
        // Keep pinned apps that are currently on home (and so not shown) after the reordered ones.
        val shown = keys.toSet()
        store.setPinned(keys + state.value.settings.pinned.filter { it !in shown })
    }
    fun addToHome(key: String) = save { store.addToHome(key) }
    fun removeFromHome(key: String) = save { store.removeFromHome(key) }
    fun pin(key: String) = save { store.pin(key) }
    fun unpin(key: String) = save { store.unpin(key) }
    fun hide(key: String) = save { store.hide(key) }
    fun unhide(key: String) = save { store.unhide(key) }
    fun rename(key: String, label: String?) = save { store.rename(key, label) }
    fun setClockMode(mode: ClockMode) = save { store.setClockMode(mode) }
    fun setKeyboardOnOpen(value: Boolean) = save { store.setKeyboardOnOpen(value) }
    fun setOpenOnlyMatch(value: Boolean) = save { store.setOpenOnlyMatch(value) }
    fun setEnterSearchesWeb(value: Boolean) = save { store.setEnterSearchesWeb(value) }
    fun setDoubleTapToLock(value: Boolean) = save { store.setDoubleTapToLock(value) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}

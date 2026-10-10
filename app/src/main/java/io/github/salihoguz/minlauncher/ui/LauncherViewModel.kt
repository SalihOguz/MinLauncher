package io.github.salihoguz.minlauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.salihoguz.minlauncher.data.AppRepository
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.data.LauncherSettings
import io.github.salihoguz.minlauncher.data.SettingsStore
import io.github.salihoguz.minlauncher.data.VisitLog
import io.github.salihoguz.minlauncher.data.VisitOutcome
import io.github.salihoguz.minlauncher.domain.AppHistory
import io.github.salihoguz.minlauncher.domain.ClockMode
import io.github.salihoguz.minlauncher.domain.HomeLayout
import io.github.salihoguz.minlauncher.domain.Sections
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
    val recent: List<LauncherApp> = emptyList(),
    val newApps: List<LauncherApp> = emptyList(),
    val hidden: List<LauncherApp> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
) {
    val homeKeys: Set<String> get() = home.mapTo(HashSet()) { it.key }
    val recentKeys: Set<String> get() = recent.mapTo(HashSet()) { it.key }
    val newKeys: Set<String> get() = newApps.mapTo(HashSet()) { it.key }
    val homeIsFull: Boolean get() = home.size >= HomeLayout.MAX_APPS
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application, viewModelScope)
    private val store = SettingsStore(application)
    private val visits = VisitLog(application)
    private val collator = Collator.getInstance()

    val state: StateFlow<UiState> = combine(repository.apps, store.settings) { installed, settings ->
        if (installed == null) return@combine UiState(settings = settings)
        val now = System.currentTimeMillis()
        val all = installed.map { app ->
            LauncherApp(
                app = app,
                label = settings.renames[app.key] ?: app.label,
                searchWords = settings.searchWords[app.key].orEmpty(),
                lastOpened = settings.lastOpened[app.key],
                showInRecent = settings.showInRecent[app.key] ?: !app.isGame,
                isFresh = Sections.isFresh(app.installedAt, now),
            )
        }
        val byKey = all.associateBy { it.key }
        val visible = all.filter { it.key !in settings.hidden }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
        val home = settings.home.mapNotNull { byKey[it] }.filter { it.key !in settings.hidden }
        val homeKeys = home.mapTo(HashSet()) { it.key }
        val pinned = settings.pinned.mapNotNull { byKey[it] }.filter { it.key !in settings.hidden && it.key !in homeKeys }
        val settled = homeKeys + pinned.map { it.key }
        val history = visible.map { app ->
            AppHistory(
                key = app.key,
                installedAt = app.app.installedAt,
                lastOpened = app.lastOpened,
                showInRecent = app.showInRecent,
                removedFromRecentAt = settings.removedFromRecent[app.key],
                dismissedFromNew = app.key in settings.dismissedFromNew,
            )
        }
        UiState(
            loaded = true,
            apps = visible,
            home = home,
            pinned = pinned,
            recent = Sections.recent(history, settled, settings.recentCount).mapNotNull { byKey[it] },
            newApps = Sections.newApps(history, settings.trackingSince, settled).mapNotNull { byKey[it] },
            hidden = all.filter { it.key in settings.hidden }.sortedWith { a, b -> collator.compare(a.label, b.label) },
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        save { store.ensureTrackingStarted() }
    }

    fun start() = repository.start()
    fun stop() = repository.stop()

    fun recordLaunch(key: String) = save { store.recordLaunch(key) }
    fun recordVisit(outcome: VisitOutcome, keyboardClosedFirst: Boolean) = save { visits.record(outcome, keyboardClosedFirst) }

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
    fun rename(key: String, label: String?, words: List<String>) = save { store.rename(key, label, words) }
    fun setShowInRecent(key: String, value: Boolean) = save { store.setShowInRecent(key, value) }
    fun removeFromRecent(key: String) = save { store.removeFromRecent(key) }
    fun dismissFromNew(key: String) = save { store.dismissFromNew(key) }
    fun setClockMode(mode: ClockMode) = save { store.setClockMode(mode) }
    fun setKeyboardOnOpen(value: Boolean) = save { store.setKeyboardOnOpen(value) }
    fun setOpenOnlyMatch(value: Boolean) = save { store.setOpenOnlyMatch(value) }
    fun setEnterSearchesWeb(value: Boolean) = save { store.setEnterSearchesWeb(value) }
    fun setDoubleTapToLock(value: Boolean) = save { store.setDoubleTapToLock(value) }
    fun setDrawerDim(value: Float) = save { store.setDrawerDim(value) }
    fun setRecentCount(value: Int) = save { store.setRecentCount(value) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}

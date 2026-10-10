package io.github.salihoguz.minlauncher.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.salihoguz.minlauncher.data.AppRepository
import io.github.salihoguz.minlauncher.data.LaunchLog
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.data.LauncherSettings
import io.github.salihoguz.minlauncher.data.SettingsStore
import io.github.salihoguz.minlauncher.data.Usage
import io.github.salihoguz.minlauncher.data.VisitLog
import io.github.salihoguz.minlauncher.domain.AppHistory
import io.github.salihoguz.minlauncher.domain.ClockMode
import io.github.salihoguz.minlauncher.domain.DayUsage
import io.github.salihoguz.minlauncher.domain.HomeLayout
import io.github.salihoguz.minlauncher.domain.Launch
import io.github.salihoguz.minlauncher.domain.LaunchSource
import io.github.salihoguz.minlauncher.domain.Sections
import io.github.salihoguz.minlauncher.domain.Visit
import io.github.salihoguz.minlauncher.domain.VisitOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    /** User folders with their apps, in the user's order. */
    val folders: List<FolderGroup> = emptyList(),
    /** Apps not filed in any folder: the inbox. */
    val unsorted: List<LauncherApp> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
) {
    val homeKeys: Set<String> get() = home.mapTo(HashSet()) { it.key }
    val recentKeys: Set<String> get() = recent.mapTo(HashSet()) { it.key }
    val newKeys: Set<String> get() = newApps.mapTo(HashSet()) { it.key }
    val homeIsFull: Boolean get() = home.size >= HomeLayout.MAX_APPS
}

data class FolderGroup(val name: String, val apps: List<LauncherApp>)

/** What Library and Stats show. Loaded when the Manage screen opens. */
data class ManageData(
    val loaded: Boolean = false,
    val usageAccess: Boolean = false,
    /** Package -> last time used, from Android's records (usage access only). */
    val lastUsed: Map<String, Long> = emptyMap(),
    /** Package -> time on screen in the last 30 days (usage access only). */
    val screenTime30d: Map<String, Long> = emptyMap(),
    val today: DayUsage? = null,
    /** App key -> bytes. Filled in after the rest, since it takes a moment. */
    val sizes: Map<String, Long> = emptyMap(),
    val visits: List<Visit> = emptyList(),
    val launches: List<Launch> = emptyList(),
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application, viewModelScope)
    private val store = SettingsStore(application)
    private val visits = VisitLog(application)
    private val launches = LaunchLog(application)
    private val usage = Usage(application)

    private val _manage = MutableStateFlow(ManageData())
    val manage: StateFlow<ManageData> = _manage
    private var manageJob: Job? = null
    private val collator = Collator.getInstance()

    val state: StateFlow<UiState> = combine(repository.apps, store.settings) { installed, settings ->
        if (installed == null) return@combine UiState(settings = settings)
        val now = System.currentTimeMillis()
        val folderNames = settings.folders.toSet()
        val all = installed.map { app ->
            LauncherApp(
                app = app,
                label = settings.renames[app.key] ?: app.label,
                searchWords = settings.searchWords[app.key].orEmpty(),
                lastOpened = settings.lastOpened[app.key],
                showInRecent = settings.showInRecent[app.key] ?: !app.isGame,
                isFresh = Sections.isFresh(app.installedAt, now),
                folder = settings.folderOf[app.key]?.takeIf { it in folderNames },
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
            folders = settings.folders.map { name -> FolderGroup(name, visible.filter { it.folder == name }) },
            unsorted = visible.filter { it.folder == null },
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        save { store.ensureTrackingStarted() }
    }

    fun start() = repository.start()
    fun stop() = repository.stop()

    fun recordLaunch(key: String, source: LaunchSource) = save {
        store.recordLaunch(key)
        launches.record(key, source)
    }
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
    fun createFolder(name: String) = save { store.createFolder(name) }
    fun moveToFolder(key: String, folder: String?) = save { store.moveToFolder(key, folder) }
    fun renameFolder(old: String, new: String) = save { store.renameFolder(old, new) }
    fun deleteFolder(name: String) = save { store.deleteFolder(name) }
    fun moveFolder(name: String, by: Int) = save { store.moveFolder(name, by) }
    fun setClockMode(mode: ClockMode) = save { store.setClockMode(mode) }
    fun setKeyboardOnOpen(value: Boolean) = save { store.setKeyboardOnOpen(value) }
    fun setOpenOnlyMatch(value: Boolean) = save { store.setOpenOnlyMatch(value) }
    fun setEnterSearchesWeb(value: Boolean) = save { store.setEnterSearchesWeb(value) }
    fun setDoubleTapToLock(value: Boolean) = save { store.setDoubleTapToLock(value) }
    fun setDrawerDim(value: Float) = save { store.setDrawerDim(value) }
    fun setRecentCount(value: Int) = save { store.setRecentCount(value) }
    fun snoozeSuggestion(id: String) = save { store.snoozeSuggestion(id) }

    /** Moves a home app into Pinned. */
    fun moveHomeToPinned(key: String) = save {
        store.removeFromHome(key)
        store.pin(key)
    }

    /** Reloads usage records, logs and sizes for Library and Stats. */
    fun refreshManage() {
        manageJob?.cancel()
        manageJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val loaded = withContext(Dispatchers.IO) {
                val access = usage.hasAccess()
                ManageData(
                    loaded = true,
                    usageAccess = access,
                    lastUsed = if (access) usage.lastUsed(now) else emptyMap(),
                    screenTime30d = if (access) usage.foregroundTime(now - 30 * Sections.DAY_MS, now) else emptyMap(),
                    today = if (access) usage.today(now) else null,
                    visits = visits.read(),
                    launches = launches.read(),
                )
            }
            // Sizes change slowly: keep the ones already known if usage access hasn't changed.
            val previous = _manage.value
            val known = if (previous.usageAccess == loaded.usageAccess) previous.sizes else emptyMap()
            _manage.value = loaded.copy(sizes = known)
            val apps = (state.value.apps + state.value.hidden).map { it.app }
            val appKeys = apps.mapTo(HashSet()) { it.key }
            val missing = apps.filter { it.key !in known }
            val sizes = HashMap(known.filterKeys { it in appKeys })
            withContext(Dispatchers.IO) {
                missing.chunked(20).forEach { chunk ->
                    ensureActive()
                    chunk.forEach { app -> usage.size(app, loaded.usageAccess)?.let { sizes[app.key] = it } }
                    val snapshot = HashMap(sizes)
                    _manage.update { it.copy(sizes = snapshot) }
                }
            }
            _manage.update { it.copy(sizes = HashMap(sizes)) }
        }
    }

    /** Writes a backup to [uri]. Reports success. */
    fun exportSettings(uri: Uri, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val json = store.exportJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            done(ok)
        }
    }

    /** Replaces all settings with the backup at [uri]. Reports success. */
    fun importSettings(uri: Uri, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
            }.getOrNull()
            done(text != null && store.importJson(text))
        }
    }
    fun setAlphabetRail(value: Boolean) = save { store.setAlphabetRail(value) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}

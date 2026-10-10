package io.github.salihoguz.minlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.domain.HomeLayout
import io.github.salihoguz.minlauncher.domain.LaunchSource
import io.github.salihoguz.minlauncher.system.Actions
import kotlinx.coroutines.flow.Flow

private enum class Screen { HOME, DRAWER, MANAGE }

@Composable
fun LauncherRoot(viewModel: LauncherViewModel, homePresses: Flow<Unit>) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val manage by viewModel.manage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var manageTab by rememberSaveable { mutableStateOf(ManageTab.LIBRARY) }
    var libraryFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var sheetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var renameKey by rememberSaveable { mutableStateOf<String?>(null) }
    // The "New folder" dialog, and the app to file in it when it was opened from an app's menu.
    var naming by rememberSaveable { mutableStateOf(false) }
    var namingFor by rememberSaveable { mutableStateOf<String?>(null) }

    // Pressing Home while the launcher is open always brings you back to the home screen.
    LaunchedEffect(homePresses) {
        homePresses.collect {
            screen = Screen.HOME
            sheetKey = null
            renameKey = null
            naming = false
            namingFor = null
        }
    }
    // A launcher has nowhere to go back to.
    BackHandler(enabled = screen == Screen.HOME) {}

    fun find(key: String?): LauncherApp? =
        key?.let { k -> state.apps.firstOrNull { it.key == k } ?: state.hidden.firstOrNull { it.key == k } }

    val open: (LauncherApp, LaunchSource) -> Unit = { app, source ->
        Actions.launch(context, app.app)
        viewModel.recordLaunch(app.key, source)
        screen = Screen.HOME
    }
    fun openManage(tab: ManageTab) {
        manageTab = tab
        if (tab == ManageTab.LIBRARY) libraryFilter = LibraryFilter.ALL
        screen = Screen.MANAGE
    }

    Box(Modifier.fillMaxSize()) {
        // One full-screen darkening layer, under everything, that fades with the screen change.
        // Kept apart from the drawer's content (which also slides a little) so the wallpaper darkens evenly.
        val dim by animateFloatAsState(
            targetValue = when (screen) {
                Screen.HOME -> state.settings.homeDim
                Screen.DRAWER -> state.settings.drawerDim
                Screen.MANAGE -> maxOf(0.6f, state.settings.homeDim)
            },
            animationSpec = tween(durationMillis = 220),
            label = "dim",
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black.copy(alpha = dim)) },
        )

        // Home fades out while the drawer or settings are open, so its text never shows through.
        AnimatedVisibility(visible = screen == Screen.HOME, enter = fadeIn(tween(220)), exit = fadeOut(tween(160))) {
            HomeScreen(
                apps = state.home,
                clockMode = state.settings.clockMode,
                loaded = state.loaded,
                actions = HomeActions(
                    onOpen = { open(it, LaunchSource.HOME) },
                    onAppLongPress = { sheetKey = it.key },
                    onReorder = viewModel::reorderHome,
                    onSwipeUp = { screen = Screen.DRAWER },
                    onSwipeDown = { Actions.expandNotifications(context) },
                    onSwipeLeft = { Actions.camera(context) },
                    onSwipeRight = { Actions.dialer(context) },
                    onDoubleTap = {
                        if (state.settings.doubleTapToLock && !Actions.lockScreen()) {
                            Actions.toast(context, "Turn on the lock service in Settings to lock with a double-tap")
                        }
                    },
                    onEmptyLongPress = { openManage(ManageTab.SETTINGS) },
                    onClock = { Actions.clock(context) },
                    onDate = { Actions.calendar(context) },
                ),
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.DRAWER,
            enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 24 },
            exit = fadeOut(tween(160)),
        ) {
            DrawerScreen(
                state = state,
                onOpen = { app, outcome -> open(app, LaunchSource.of(outcome)) },
                onLongPress = { sheetKey = it.key },
                onReorderPinned = viewModel::reorderPinned,
                onWebSearch = { query ->
                    Actions.webSearch(context, query)
                    screen = Screen.HOME
                },
                onNewFolder = {
                    namingFor = null
                    naming = true
                },
                onManage = { openManage(ManageTab.LIBRARY) },
                onVisitEnd = viewModel::recordVisit,
                onClose = { screen = Screen.HOME },
            )
        }

        AnimatedVisibility(visible = screen == Screen.MANAGE, enter = fadeIn(), exit = fadeOut()) {
            ManageScreen(
                state = state,
                viewModel = viewModel,
                tab = manageTab,
                onTab = { manageTab = it },
                filter = libraryFilter,
                onFilter = { libraryFilter = it },
                onAppDetails = { sheetKey = it.key },
                onClose = { screen = Screen.HOME },
            )
        }

        find(sheetKey)?.let { app ->
            val onHome = app.key in state.homeKeys
            val pinned = app.key in state.settings.pinned
            val hidden = app.key in state.settings.hidden
            AppSheet(
                app = app,
                onHome = onHome,
                pinned = pinned,
                hidden = hidden,
                inNew = app.key in state.newKeys,
                inRecent = app.key in state.recentKeys,
                folders = state.settings.folders,
                size = manage.sizes[app.key],
                onDismiss = { sheetKey = null },
                onMoveToFolder = { folder ->
                    viewModel.moveToFolder(app.key, folder)
                    sheetKey = null
                },
                onNewFolder = {
                    sheetKey = null
                    namingFor = app.key
                    naming = true
                },
                onAction = { action ->
                    sheetKey = null
                    when (action) {
                        SheetAction.OPEN -> open(app, LaunchSource.SHEET)
                        SheetAction.APP_INFO -> Actions.appInfo(context, app.app)
                        // Apps that came with the phone can only be disabled, in App info.
                        SheetAction.UNINSTALL -> if (app.app.isSystem) Actions.appInfo(context, app.app) else Actions.uninstall(context, app.app)
                        SheetAction.RENAME -> renameKey = app.key
                        SheetAction.TOGGLE_HIDDEN -> if (hidden) viewModel.unhide(app.key) else viewModel.hide(app.key)
                        SheetAction.REMOVE_FROM_NEW -> viewModel.dismissFromNew(app.key)
                        SheetAction.REMOVE_FROM_RECENT -> viewModel.removeFromRecent(app.key)
                        SheetAction.TOGGLE_SHOW_IN_RECENT -> {
                            viewModel.setShowInRecent(app.key, !app.showInRecent)
                            sheetKey = app.key
                        }
                        SheetAction.TOGGLE_PIN -> when {
                            pinned -> viewModel.unpin(app.key)
                            onHome -> Actions.toast(context, "${app.label} is on home already")
                            else -> viewModel.pin(app.key)
                        }
                        SheetAction.TOGGLE_HOME -> when {
                            onHome -> viewModel.removeFromHome(app.key)
                            state.homeIsFull -> Actions.toast(context, "Home is full at ${HomeLayout.MAX_APPS} apps")
                            else -> viewModel.addToHome(app.key)
                        }
                    }
                },
            )
        }

        find(renameKey)?.let { app ->
            RenameDialog(
                app = app,
                onDismiss = { renameKey = null },
                onSave = { label, words ->
                    viewModel.rename(app.key, label, words)
                    renameKey = null
                },
            )
        }

        if (naming) {
            FolderNameDialog(
                title = "New folder",
                initial = "",
                existing = state.settings.folders,
                renaming = null,
                onDismiss = {
                    naming = false
                    namingFor = null
                },
                onSave = { name ->
                    val key = namingFor
                    if (key != null) viewModel.moveToFolder(key, name) else viewModel.createFolder(name)
                    naming = false
                    namingFor = null
                },
            )
        }
    }
}

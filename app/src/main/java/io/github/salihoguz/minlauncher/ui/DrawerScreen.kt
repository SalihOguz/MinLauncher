@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package io.github.salihoguz.minlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.domain.Rail
import io.github.salihoguz.minlauncher.domain.Search
import io.github.salihoguz.minlauncher.domain.VisitOutcome
import kotlinx.coroutines.launch

@Composable
fun DrawerScreen(
    state: UiState,
    onOpen: (LauncherApp, VisitOutcome) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
    onReorderPinned: (List<String>) -> Unit,
    onWebSearch: (String) -> Unit,
    onNewFolder: () -> Unit,
    onManage: () -> Unit,
    onVisitEnd: (VisitOutcome, keyboardClosedFirst: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val settings = state.settings
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, state.apps) {
        if (query.isBlank()) emptyList() else Search.filter(state.apps, query, searchWords = { it.searchWords }) { it.label }
    }
    val expanded = remember { mutableStateListOf<String>() }
    val expandedNow = expanded.toList()
    val drawer = remember(state, query, results, expandedNow) { buildRows(state, query, results, expandedNow) }
    val railTargets = remember(state.apps) { Rail.targets(state.apps.map { it.label }) }
    val availableLetters = remember(state.apps) { state.apps.mapTo(HashSet()) { Rail.letterOf(it.label) } }
    // Opening from All apps after using the rail counts as a rail visit.
    var usedRail by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // One record per visit: how it ended, and whether the keyboard was put away first.
    val visit = remember { VisitTracker() }
    val currentVisitEnd by rememberUpdatedState(onVisitEnd)
    DisposableEffect(Unit) {
        onDispose { visit.end(VisitOutcome.NONE, currentVisitEnd) }
    }
    fun open(app: LauncherApp, outcome: VisitOutcome) {
        val counted = if (outcome == VisitOutcome.LIST && usedRail) VisitOutcome.RAIL else outcome
        visit.end(counted, currentVisitEnd)
        onOpen(app, counted)
    }
    val searchWeb: (String) -> Unit = { text ->
        visit.end(VisitOutcome.WEB, currentVisitEnd)
        onWebSearch(text)
    }

    BackHandler(onBack = onClose)

    LaunchedEffect(Unit) {
        if (settings.keyboardOnOpen) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // The list never resizes with the keyboard: the keyboard and the search bar float over it,
    // and the list keeps room for the keyboard below its last row.
    // - When MinLauncher puts the keyboard away (you scrolled or tapped the list), that room
    //   stays, so nothing moves under your finger.
    // - When you put it away yourself (Back), the list follows it down to the search bar.
    // The keyboard's position is read in derived states, not in composition, so the drawer
    // doesn't recompose on every frame of the keyboard sliding in (that made opening laggy).
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    // Where the keyboard is heading: its full height while opening, 0 while closing.
    val targetRoom by remember(density, navBottom) {
        derivedStateOf { (imeTarget.getBottom(density) - navBottom).coerceAtLeast(0) }
    }
    val keyboardUp by remember(density, navBottom) { derivedStateOf { ime.getBottom(density) > navBottom } }
    var keyboardRoom by remember { mutableIntStateOf(0) }
    var holdRoom by remember { mutableStateOf(false) }
    LaunchedEffect(targetRoom) {
        when {
            targetRoom >= keyboardRoom -> keyboardRoom = targetRoom
            !holdRoom -> {
                // Put away with Back: the list comes down to the search bar.
                keyboardRoom = targetRoom
                listState.animateScrollToItem(0)
            }
        }
        // Opening again: whoever closes it next decides what happens.
        if (targetRoom > 0) holdRoom = false
    }

    fun putKeyboardAway() {
        if (keyboardUp) {
            holdRoom = true
            visit.keyboardClosedFirst = true
        }
        keyboard?.hide()
        focusManager.clearFocus()
    }

    // Dragging the list puts the keyboard away, so you can browse.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) putKeyboardAway() }
    }
    LaunchedEffect(results) {
        if (settings.openOnlyMatch && query.isNotBlank() && results.size == 1) open(results.single(), VisitOutcome.SEARCH)
    }

    val onEnter = {
        if (query.isNotBlank()) {
            if (settings.enterSearchesWeb || results.isEmpty()) searchWeb(query) else open(results.first(), VisitOutcome.SEARCH)
        }
    }

    val rowFallbackPx = with(density) { 50.dp.roundToPx() }
    fun jumpTo(letter: Char) {
        usedRail = true
        scope.launch {
            if (letter == RAIL_BOTTOM) {
                listState.scrollToItem(0)
                return@launch
            }
            val position = railTargets[letter] ?: return@launch
            val row = drawer.allAppsStart + (state.apps.size - 1 - position)
            listState.showAtTop(row, rowFallbackPx)
        }
    }

    // Where the search bar starts, in screen coordinates. The list fades out just above it,
    // so names never show through the bar, wherever the keyboard puts it.
    var barTop by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }
    var listTop by remember { mutableFloatStateOf(0f) }

    // The darkening is drawn by LauncherRoot as its own layer, so it fades in evenly.
    Box(Modifier.fillMaxSize()) {
        // Row 0 sits on the search bar; the list grows upward from your thumb.
        LazyColumn(
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(
                start = 24.dp,
                end = if (settings.alphabetRail) 40.dp else 24.dp,
                top = 8.dp,
                bottom = with(density) { keyboardRoom.toDp() } + FadeHeight,
            ),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = SearchBarHeight)
                // A tap on empty space puts the keyboard away and leaves the list where it is.
                .pointerInput(Unit) { detectTapGestures { putKeyboardAway() } }
                .onGloballyPositioned { listTop = it.positionInRoot().y }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val edge = barTop - listTop
                    if (edge < size.height) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                1f to Color.Transparent,
                                startY = edge - FadeHeight.toPx(),
                                endY = edge,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                },
        ) {
            items(drawer.rows, key = { it.key }, contentType = { it::class }) { row ->
                when (row) {
                    is DrawerRow.Web -> WebRow(row.query) { searchWeb(row.query) }
                    is DrawerRow.App -> AppRow(row.app, { open(row.app, row.outcome) }, onLongPress, inFolder = row.inFolder)
                    is DrawerRow.Recent -> RecentGrid(row.apps, { open(it, VisitOutcome.RECENT) }, onLongPress)
                    is DrawerRow.Pinned -> PinnedGrid(row.apps, { open(it, VisitOutcome.PINNED) }, onLongPress, onReorderPinned)
                    is DrawerRow.Folder -> FolderRow(row.name, row.count, row.open) {
                        if (row.open) expanded.remove(row.name) else expanded.add(row.name)
                    }
                    is DrawerRow.NewFolder -> NewFolderRow(onNewFolder)
                    is DrawerRow.Label -> SectionLabel(row.text)
                }
            }
        }

        // With the keyboard down, the A–Z rail runs down the right edge.
        AnimatedVisibility(
            visible = settings.alphabetRail && !keyboardUp && query.isBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = SearchBarHeight + 24.dp, end = 6.dp),
        ) {
            AlphabetRail(available = availableLetters, onLetter = ::jumpTo)
        }

        SearchBar(
            query = query,
            onQueryChange = { query = it },
            focusRequester = focusRequester,
            onEnter = onEnter,
            onManage = onManage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .onGloballyPositioned { barTop = it.positionInRoot().y },
        )
    }
}

/** One row of the drawer list. Rows are listed bottom-up: the first one sits on the search bar. */
private sealed interface DrawerRow {
    val key: String

    data class Label(val text: String, override val key: String) : DrawerRow

    data class Web(val query: String) : DrawerRow {
        override val key get() = "web"
    }

    data class Pinned(val apps: List<LauncherApp>) : DrawerRow {
        override val key get() = "pinned"
    }

    data class Recent(val apps: List<LauncherApp>) : DrawerRow {
        override val key get() = "recent"
    }

    data class App(
        val app: LauncherApp,
        val outcome: VisitOutcome,
        override val key: String,
        /** Inside an open folder: joined to it by a line. */
        val inFolder: Boolean = false,
    ) : DrawerRow

    data class Folder(val name: String, val count: Int, val open: Boolean) : DrawerRow {
        override val key get() = "folder-$name"
    }

    data object NewFolder : DrawerRow {
        override val key get() = "new-folder"
    }
}

/** The rows, and where All apps starts (the row of its last app, alphabetically). */
private class DrawerRows(val rows: List<DrawerRow>, val allAppsStart: Int)

private fun buildRows(state: UiState, query: String, results: List<LauncherApp>, expanded: List<String>): DrawerRows {
    val rows = ArrayList<DrawerRow>()
    if (query.isNotBlank()) {
        rows += DrawerRow.Web(query)
        results.forEach { rows += DrawerRow.App(it, VisitOutcome.SEARCH, "result-" + it.key) }
        return DrawerRows(rows, allAppsStart = 0)
    }
    if (state.pinned.isNotEmpty()) rows += DrawerRow.Pinned(state.pinned)
    if (state.recent.isNotEmpty()) rows += DrawerRow.Recent(state.recent)
    if (state.newApps.isNotEmpty()) {
        // Newest first, so the newest sits at the bottom, nearest your thumb.
        state.newApps.forEach { rows += DrawerRow.App(it, VisitOutcome.NEW, "new-" + it.key) }
        rows += DrawerRow.Label("New", "new-label")
    }
    rows += DrawerRow.NewFolder
    for (group in state.folders.asReversed()) {
        val open = group.name in expanded
        if (open) {
            group.apps.asReversed().forEach {
                rows += DrawerRow.App(it, VisitOutcome.FOLDER, "f-${group.name}-${it.key}", inFolder = true)
            }
        }
        rows += DrawerRow.Folder(group.name, group.apps.size, open)
    }
    rows += DrawerRow.Label("Folders", "folders-label")
    val allAppsStart = rows.size
    state.apps.asReversed().forEach { rows += DrawerRow.App(it, VisitOutcome.LIST, "all-" + it.key) }
    rows += DrawerRow.Label("All apps · ${state.apps.size}", "all-label")
    return DrawerRows(rows, allAppsStart)
}

/** Scrolls so the row at [index] is the top one on screen (rows grow upward from the bottom). */
private suspend fun LazyListState.showAtTop(index: Int, fallbackRowPx: Int) {
    val info = layoutInfo
    val rowPx = info.visibleItemsInfo.firstOrNull { it.key.toString().startsWith("all-") }?.size ?: fallbackRowPx
    val usable = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
    val fit = (usable / rowPx.coerceAtLeast(1)).coerceAtLeast(1)
    scrollToItem((index - fit + 1).coerceAtLeast(0))
}

/** Remembers how a drawer visit ended; only the first ending counts. */
private class VisitTracker {
    var keyboardClosedFirst = false
    private var ended = false

    fun end(outcome: VisitOutcome, report: (VisitOutcome, Boolean) -> Unit) {
        if (ended) return
        ended = true
        report(outcome, keyboardClosedFirst)
    }
}

private val SearchBarHeight = 64.dp
private val FadeHeight = 28.dp
private val IconGap = 14.dp
/** Apps in an open folder line up with the folder's name. */
private val FolderIndent = IconSize + IconGap
private val GuideColor = Color.White.copy(alpha = 0.28f)

/** The line joining an open folder to its apps, down the middle of the folder icon. */
private fun DrawScope.folderGuide(fromY: Float) {
    val x = IconSize.toPx() / 2f
    drawLine(GuideColor, Offset(x, fromY), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onEnter: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .height(SearchBarHeight)
            .padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search apps or the web", style = Ui.drawerApp.copy(color = Ui.faint), maxLines = 1)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = Ui.drawerApp,
                    cursorBrush = SolidColor(Ui.text),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onEnter() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            if (query.isNotEmpty()) {
                Text(
                    text = "clear",
                    color = Ui.dim,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onQueryChange("") }
                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                )
            } else {
                // Library, Stats and Settings.
                ManageGlyph(
                    Modifier
                        .clickable(onClick = onManage)
                        .padding(start = 14.dp, end = 2.dp, top = 10.dp, bottom = 10.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ui.faint),
        )
    }
}

@Composable
private fun AppRow(
    app: LauncherApp,
    onOpen: () -> Unit,
    onLongPress: (LauncherApp) -> Unit,
    inFolder: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (inFolder) Modifier.drawBehind { folderGuide(fromY = 0f) } else Modifier)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress(app)
                },
                onClick = onOpen,
            )
            .padding(start = if (inFolder) FolderIndent else 0.dp, top = 10.dp, bottom = 10.dp),
    ) {
        AppIcon(app.app)
        Spacer(Modifier.width(IconGap))
        Text(
            text = nameWithMarker(app),
            style = Ui.drawerApp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A folder: closed, or open with its apps joined below it by a line. */
@Composable
private fun FolderRow(name: String, count: Int, open: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (open) Modifier.drawBehind { folderGuide(fromY = size.height / 2f + IconSize.toPx() * 0.4f) } else Modifier)
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
    ) {
        FolderGlyph(open = open)
        Spacer(Modifier.width(IconGap))
        Text(name, style = Ui.drawerApp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text("  $count", color = Ui.faint, fontSize = 14.sp)
    }
}

@Composable
private fun NewFolderRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        PlusGlyph()
        Spacer(Modifier.width(IconGap))
        Text("New folder", style = Ui.drawerApp.copy(color = Ui.dim))
    }
}

/** The app's name, with a small ✦ for the first 3 days after install. */
fun nameWithMarker(app: LauncherApp): AnnotatedString = buildAnnotatedString {
    append(app.label)
    if (app.isFresh) {
        withStyle(SpanStyle(color = Ui.highlight, fontSize = 16.sp)) { append("  ✦") }
    }
}

@Composable
private fun WebRow(query: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(1.dp)
                .background(Ui.text.copy(alpha = 0.1f)),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        ) {
            SearchGlyph()
            Spacer(Modifier.width(IconGap))
            Text(
                text = "Search the web for “${query.trim()}”",
                style = Ui.drawerApp.copy(fontSize = 18.sp, color = Ui.dim),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = Ui.label,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
    )
}

/**
 * Recent apps in two text columns, like Pinned, without icons. The most recent is at the bottom
 * left, then bottom right, then the row above.
 */
@Composable
private fun RecentGrid(apps: List<LauncherApp>, onOpen: (LauncherApp) -> Unit, onLongPress: (LauncherApp) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("Recent")
        val rows = apps.chunked(2)
        for (row in rows.asReversed()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                for (column in 0..1) {
                    val app = row.getOrNull(column)
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .then(
                                if (app == null) Modifier else Modifier.combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPress(app)
                                    },
                                    onClick = { onOpen(app) },
                                ),
                            ),
                    ) {
                        if (app != null) {
                            Text(app.label, style = Ui.drawerApp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private const val TAP = 1
private const val MOVED = 2
private const val LONG_PRESS = 3

/** Pinned apps in two text columns. Hold and drag to reorder; hold and release for the sheet. */
@Composable
private fun PinnedGrid(
    apps: List<LauncherApp>,
    onOpen: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val open by rememberUpdatedState(onOpen)
    val longPress by rememberUpdatedState(onLongPress)
    val reorder by rememberUpdatedState(onReorder)

    var order by remember { mutableStateOf(apps) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(apps) { if (dragKey == null) order = apps }

    Column(Modifier.fillMaxWidth()) {
        SectionLabel("Pinned")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 12.dp
            val cellWidth = (maxWidth - gap) / 2
            val cellHeight = 50.dp
            val rows = (order.size + 1) / 2
            val cellWidthPx = with(density) { cellWidth.toPx() }
            val cellHeightPx = with(density) { cellHeight.toPx() }
            val gapPx = with(density) { gap.toPx() }

            fun cellAt(pos: Offset, clamp: Boolean): Int? {
                val column = if (pos.x >= cellWidthPx + gapPx / 2f) 1 else 0
                val row = (pos.y / cellHeightPx).toInt()
                val index = row * 2 + column
                return when {
                    clamp -> index.coerceIn(0, (order.size - 1).coerceAtLeast(0))
                    pos.y < 0f || index >= order.size -> null
                    else -> index
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cellHeight * rows)
                    .pointerInput(cellWidthPx, cellHeightPx) {
                        val slop = viewConfiguration.touchSlop
                        val longPressMs = viewConfiguration.longPressTimeoutMillis
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val start = down.position
                            val hit = cellAt(start, clamp = false) ?: return@awaitEachGesture

                            val outcome = withTimeoutOrNull(longPressMs) {
                                var result = 0
                                while (result == 0) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                    result = when {
                                        change == null || !change.pressed -> TAP
                                        (change.position - start).getDistance() > slop -> MOVED
                                        else -> 0
                                    }
                                }
                                result
                            } ?: LONG_PRESS

                            when (outcome) {
                                TAP -> order.getOrNull(hit)?.let { open(it) }
                                MOVED -> Unit // Let the list scroll.
                                else -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragKey = order.getOrNull(hit)?.key
                                    var from = hit
                                    var moved = false
                                    while (true) {
                                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                        change.consume()
                                        if (!change.pressed) break
                                        if ((change.position - start).getDistance() > slop) moved = true
                                        if (moved) {
                                            val to = cellAt(change.position, clamp = true) ?: from
                                            if (to != from && to in order.indices) {
                                                order = order.toMutableList().apply { add(to, removeAt(from)) }
                                                from = to
                                            }
                                        }
                                    }
                                    dragKey = null
                                    if (moved) reorder(order.map { it.key })
                                    else order.getOrNull(from)?.let { longPress(it) }
                                }
                            }
                        }
                    },
            ) {
                order.forEachIndexed { index, app ->
                    key(app.key) {
                        val x by animateDpAsState((cellWidth + gap) * (index % 2), label = "x")
                        val y by animateDpAsState(cellHeight * (index / 2), label = "y")
                        Text(
                            text = app.label,
                            style = Ui.drawerApp.copy(color = if (app.key == dragKey) Ui.highlight else Ui.text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .offset(x = x, y = y)
                                .width(cellWidth)
                                .height(cellHeight)
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
    }
}

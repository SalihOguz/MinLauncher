@file:OptIn(ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
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
import io.github.salihoguz.minlauncher.data.VisitOutcome
import io.github.salihoguz.minlauncher.domain.Folders
import io.github.salihoguz.minlauncher.domain.Search

@Composable
fun DrawerScreen(
    state: UiState,
    onOpen: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
    onReorderPinned: (List<String>) -> Unit,
    onWebSearch: (String) -> Unit,
    onNewFolder: () -> Unit,
    onVisitEnd: (VisitOutcome, keyboardClosedFirst: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val settings = state.settings
    var query by rememberSaveable { mutableStateOf("") }
    // A letter picked on the A–Z rail filters the list until you tap empty space or clear.
    var letter by remember { mutableStateOf<Char?>(null) }
    val results = remember(query, state.apps) {
        if (query.isBlank()) emptyList() else Search.filter(state.apps, query, searchWords = { it.searchWords }) { it.label }
    }
    val lettered = remember(letter, state.apps) {
        letter?.let { l -> state.apps.filter { Folders.railLetter(it.label) == l } }.orEmpty()
    }
    val availableLetters = remember(state.apps) { state.apps.mapTo(HashSet()) { Folders.railLetter(it.label) } }
    val expanded = remember { mutableStateListOf<String>() }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // One record per visit: how it ended, and whether the keyboard was put away first.
    val visit = remember { VisitTracker() }
    val currentVisitEnd by rememberUpdatedState(onVisitEnd)
    DisposableEffect(Unit) {
        onDispose { visit.end(VisitOutcome.NONE, currentVisitEnd) }
    }
    fun openFrom(outcome: VisitOutcome): (LauncherApp) -> Unit = { app ->
        visit.end(outcome, currentVisitEnd)
        onOpen(app)
    }
    val searchWeb: (String) -> Unit = { text ->
        visit.end(VisitOutcome.WEB, currentVisitEnd)
        onWebSearch(text)
    }

    BackHandler {
        if (letter != null) letter = null else onClose()
    }

    LaunchedEffect(Unit) {
        if (settings.keyboardOnOpen) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // The list never resizes with the keyboard: the keyboard and the search bar float over it.
    // The room the keyboard takes is remembered for the whole visit, so when the keyboard
    // goes away (for example when you start scrolling) nothing moves under your finger.
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    var keyboardRoom by remember { mutableIntStateOf(0) }
    val room = (imeBottom - navBottom).coerceAtLeast(0)
    SideEffect { if (room > keyboardRoom) keyboardRoom = room }
    val keyboardUp by rememberUpdatedState(room > 0)

    // Scrolling the list puts the keyboard away, so you can browse.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) {
                if (keyboardUp) visit.keyboardClosedFirst = true
                keyboard?.hide()
                focusManager.clearFocus()
            }
        }
    }
    LaunchedEffect(results) {
        if (settings.openOnlyMatch && query.isNotBlank() && results.size == 1) openFrom(VisitOutcome.SEARCH)(results.single())
    }
    // Typing replaces a rail filter.
    LaunchedEffect(query) { if (query.isNotBlank()) letter = null }

    val onEnter = {
        if (query.isNotBlank()) {
            if (settings.enterSearchesWeb || results.isEmpty()) searchWeb(query) else openFrom(VisitOutcome.SEARCH)(results.first())
        }
    }

    // Where the search bar starts, in screen coordinates. The list fades out just above it,
    // so names never show through the bar, wherever the keyboard puts it.
    var barTop by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }
    var listTop by remember { mutableFloatStateOf(0f) }

    // The darkening is drawn by LauncherRoot as its own layer, so it fades in evenly.
    Box(Modifier.fillMaxSize()) {
        // Item 0 sits on the search bar; the list grows upward from your thumb.
        // Sections are emitted bottom-up: items reversed, then their label.
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
                // A tap on empty space clears the rail filter; taps on rows never reach here.
                .pointerInput(Unit) { detectTapGestures { letter = null } }
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
            val currentLetter = letter
            when {
                query.isNotBlank() -> {
                    item(key = "web") { WebRow(query) { searchWeb(query) } }
                    val open = openFrom(VisitOutcome.SEARCH)
                    items(results, key = { "result-" + it.key }) { app -> AppRow(app, open, onLongPress) }
                }
                currentLetter != null -> {
                    val open = openFrom(VisitOutcome.RAIL)
                    if (lettered.isEmpty()) {
                        item(key = "letter-empty") { Hint("No apps under $currentLetter. Tap empty space to go back.") }
                    }
                    items(lettered.asReversed(), key = { "letter-" + it.key }) { app -> AppRow(app, open, onLongPress) }
                    item(key = "letter-label") { SectionLabel("Starts with $currentLetter") }
                }
                else -> {
                    if (state.pinned.isNotEmpty()) {
                        item(key = "pinned") {
                            PinnedGrid(state.pinned, openFrom(VisitOutcome.PINNED), onLongPress, onReorderPinned)
                        }
                    }
                    if (state.recent.isNotEmpty()) {
                        val open = openFrom(VisitOutcome.RECENT)
                        items(state.recent.asReversed(), key = { "recent-" + it.key }) { app -> AppRow(app, open, onLongPress) }
                        item(key = "recent-label") { SectionLabel("Recent") }
                    }
                    if (state.newApps.isNotEmpty()) {
                        val open = openFrom(VisitOutcome.NEW)
                        items(state.newApps.asReversed(), key = { "new-" + it.key }) { app -> AppRow(app, open, onLongPress) }
                        item(key = "new-label") { SectionLabel("New") }
                    }

                    // Folders, bottom-up: New folder, Unsorted, then the user's folders in reverse.
                    val openInFolder = openFrom(VisitOutcome.FOLDER)
                    item(key = "new-folder") { NewFolderRow(onNewFolder) }
                    if (state.folders.isNotEmpty()) {
                        val groups = state.folders + FolderGroup(Folders.UNSORTED, state.unsorted)
                        for (group in groups.asReversed()) {
                            val isOpen = group.name in expanded
                            if (isOpen) {
                                items(group.apps.asReversed(), key = { "f-${group.name}-${it.key}" }) { app ->
                                    AppRow(app, openInFolder, onLongPress, indent = true)
                                }
                            }
                            item(key = "folder-${group.name}") {
                                FolderRow(group.name, group.apps.size, isOpen) {
                                    if (isOpen) expanded.remove(group.name) else expanded.add(group.name)
                                }
                            }
                        }
                    }
                    item(key = "folders-label") { SectionLabel("Folders") }

                    val open = openFrom(VisitOutcome.LIST)
                    items(state.apps.asReversed(), key = { "all-" + it.key }) { app -> AppRow(app, open, onLongPress) }
                    item(key = "all-label") { SectionLabel("All apps · ${state.apps.size}") }
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
            AlphabetRail(available = availableLetters, onLetter = { letter = it })
        }

        SearchBar(
            query = query,
            onQueryChange = { query = it },
            filterLetter = letter,
            onClearFilter = { letter = null },
            focusRequester = focusRequester,
            onEnter = onEnter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .onGloballyPositioned { barTop = it.positionInRoot().y },
        )
    }
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

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterLetter: Char?,
    onClearFilter: () -> Unit,
    focusRequester: FocusRequester,
    onEnter: () -> Unit,
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
            if (query.isNotEmpty() || filterLetter != null) {
                Text(
                    text = "clear",
                    color = Ui.dim,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                            onQueryChange("")
                            onClearFilter()
                        }
                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
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
    onOpen: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
    indent: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress(app)
                },
                onClick = { onOpen(app) },
            )
            .padding(start = if (indent) 20.dp else 0.dp, top = 10.dp, bottom = 10.dp),
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

@Composable
private fun FolderRow(name: String, count: Int, open: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
    ) {
        FolderGlyph()
        Spacer(Modifier.width(IconGap))
        Text(name, style = Ui.drawerApp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text("  $count", color = Ui.faint, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(if (open) "⌄" else "›", color = Ui.faint, fontSize = 22.sp)
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

@Composable
private fun Hint(text: String) {
    Text(text, color = Ui.dim, fontSize = 15.sp, modifier = Modifier.padding(vertical = 12.dp))
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

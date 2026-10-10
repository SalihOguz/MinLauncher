package io.github.salihoguz.minlauncher.ui

import android.text.format.DateFormat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.domain.ClockMode
import io.github.salihoguz.minlauncher.domain.HomeLayout
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class HomeActions(
    val onOpen: (LauncherApp) -> Unit,
    val onAppLongPress: (LauncherApp) -> Unit,
    val onReorder: (List<String>) -> Unit,
    val onSwipeUp: () -> Unit,
    val onSwipeDown: () -> Unit,
    val onSwipeLeft: () -> Unit,
    val onSwipeRight: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onEmptyLongPress: () -> Unit,
    val onClock: () -> Unit,
    val onDate: () -> Unit,
)

/** Olauncher's numbers, in dp and sp. */
private val MaxRowHeight = 60.dp
private val SideMargin = 24.dp
private val BottomPadding = 48.dp
private val ColumnGap = 12.dp
private const val NameSizeSp = 30f

/** Plain values the gesture handler reads; updated after each layout. */
private class HomeGeometry {
    var rowHeight = 1f
    var columnWidth = 1f
    var gap = 0f
    var listTop = 0f
    var rows = 0
    var columns = 1
    var count = 0
    var showClock = true
}

private const val TAP = 1
private const val MOVED = 2
private const val LONG_PRESS = 3

@Composable
fun HomeScreen(
    apps: List<LauncherApp>,
    clockMode: ClockMode,
    loaded: Boolean,
    actions: HomeActions,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(actions)

    var order by remember { mutableStateOf(apps) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(apps) { if (dragKey == null) order = apps }

    val count = order.size
    val columns = HomeLayout.columns(count)
    val rows = HomeLayout.rows(count)
    val showClock = HomeLayout.showClock(clockMode, count)

    val geometry = remember { HomeGeometry() }
    val labelWidths = remember { mutableStateMapOf<String, Int>() }
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var area by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var clockBox by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dateBox by remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun toArea(rootPos: Offset): Offset? {
        val r = root ?: return null
        val a = area ?: return null
        if (!r.isAttached || !a.isAttached) return null
        return a.localPositionOf(r, rootPos)
    }

    fun isInside(box: LayoutCoordinates?, rootPos: Offset): Boolean {
        val r = root ?: return false
        if (box == null || !box.isAttached || !r.isAttached) return false
        val p = box.localPositionOf(r, rootPos)
        return p.x >= 0f && p.y >= 0f && p.x <= box.size.width && p.y <= box.size.height
    }

    /** The app whose name is under [rootPos], or null for empty space. */
    fun appAt(rootPos: Offset, slack: Float): Int? {
        val p = toArea(rootPos) ?: return null
        val g = geometry
        if (g.count == 0) return null
        val y = p.y - g.listTop
        if (y < 0f || y >= g.rowHeight * g.rows) return null
        val row = (y / g.rowHeight).toInt()
        val column = if (g.columns == 2 && p.x >= g.columnWidth + g.gap / 2f) 1 else 0
        val index = column * g.rows + row
        if (index >= g.count) return null
        val left = column * (g.columnWidth + g.gap)
        val width = order.getOrNull(index)?.let { labelWidths[it.key] } ?: g.columnWidth.toInt()
        return if (p.x >= left - slack && p.x <= left + width + slack) index else null
    }

    /** The slot a dragged app would move to. */
    fun slotAt(rootPos: Offset): Int {
        val g = geometry
        val p = toArea(rootPos) ?: return 0
        val row = ((p.y - g.listTop) / g.rowHeight).toInt()
        val column = if (g.columns == 2 && p.x >= g.columnWidth + g.gap / 2f) 1 else 0
        return HomeLayout.indexAt(column, row, g.count)
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { root = it }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                val longPressMs = viewConfiguration.longPressTimeoutMillis
                val doubleTapMs = viewConfiguration.doubleTapTimeoutMillis
                val swipeDistance = 48.dp.toPx()
                val tapSlack = 16.dp.toPx()
                var lastTapTime = 0L
                var lastTapPos = Offset.Zero

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position
                    val hit = appAt(start, tapSlack)

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
                        TAP -> when {
                            hit != null -> order.getOrNull(hit)?.let { current.onOpen(it) }
                            geometry.showClock && isInside(clockBox, start) -> current.onClock()
                            isInside(dateBox, start) -> current.onDate()
                            down.uptimeMillis - lastTapTime <= doubleTapMs &&
                                (start - lastTapPos).getDistance() < slop * 4 -> {
                                lastTapTime = 0L
                                current.onDoubleTap()
                            }
                            else -> {
                                lastTapTime = down.uptimeMillis
                                lastTapPos = start
                            }
                        }

                        MOVED -> {
                            var end = start
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                end = change.position
                                change.consume()
                                if (!change.pressed) break
                            }
                            val d = end - start
                            if (max(abs(d.x), abs(d.y)) >= swipeDistance) {
                                if (abs(d.x) > abs(d.y)) {
                                    if (d.x < 0) current.onSwipeLeft() else current.onSwipeRight()
                                } else {
                                    if (d.y < 0) current.onSwipeUp() else current.onSwipeDown()
                                }
                            }
                        }

                        else -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (hit == null || hit >= order.size) {
                                // Wait for the finger to lift, then open settings.
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    change.consume()
                                    if (!change.pressed) break
                                }
                                current.onEmptyLongPress()
                            } else {
                                dragKey = order[hit].key
                                var from = hit
                                var moved = false
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    change.consume()
                                    if (!change.pressed) break
                                    if ((change.position - start).getDistance() > slop) moved = true
                                    if (moved) {
                                        val to = slotAt(change.position)
                                        if (to != from && to in order.indices) {
                                            order = order.toMutableList().apply { add(to, removeAt(from)) }
                                            from = to
                                        }
                                    }
                                }
                                dragKey = null
                                if (moved) current.onReorder(order.map { it.key })
                                else order.getOrNull(from)?.let { current.onAppLongPress(it) }
                            }
                        }
                    }
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Header(
                showClock = showClock,
                onClockPlaced = { clockBox = it },
                onDatePlaced = { dateBox = it },
            )
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = SideMargin, end = SideMargin, top = 16.dp, bottom = BottomPadding)
                    .onGloballyPositioned { area = it },
            ) {
                val rowHeight: Dp = if (rows == 0) MaxRowHeight else minOf(MaxRowHeight, maxHeight / rows)
                val columnWidth: Dp = if (columns == 2) (maxWidth - ColumnGap) / 2 else maxWidth
                val listTop: Dp = ((maxHeight - rowHeight * rows) / 2).coerceAtLeast(0.dp)
                // Spacing shrinks first; the name only shrinks once a row can't hold 30 sp.
                val nameSize = with(density) { minOf(NameSizeSp.sp.toPx(), rowHeight.toPx() * 0.65f).toSp() }

                SideEffect {
                    with(density) {
                        geometry.rowHeight = rowHeight.toPx()
                        geometry.columnWidth = columnWidth.toPx()
                        geometry.gap = ColumnGap.toPx()
                        geometry.listTop = listTop.toPx()
                    }
                    geometry.rows = rows
                    geometry.columns = columns
                    geometry.count = count
                    geometry.showClock = showClock
                }

                if (loaded && count == 0) {
                    Text(
                        text = "Swipe up for all your apps.\nHold one there and choose Add to home.",
                        style = Ui.homeApp.copy(fontSize = 18.sp, color = Ui.dim),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }

                order.forEachIndexed { index, app ->
                    key(app.key) {
                        val (column, row) = HomeLayout.cellOf(index, count)
                        val x by animateDpAsState((columnWidth + ColumnGap) * column, label = "x")
                        val y by animateDpAsState(listTop + rowHeight * row, label = "y")
                        Text(
                            text = app.label,
                            style = Ui.homeApp.copy(
                                fontSize = nameSize,
                                color = if (app.key == dragKey) Ui.highlight else Ui.text,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .offset(x = x, y = y)
                                .height(rowHeight)
                                .widthIn(max = columnWidth)
                                .wrapContentHeight(Alignment.CenterVertically)
                                .onSizeChanged { labelWidths[app.key] = it.width },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    showClock: Boolean,
    onClockPlaced: (LayoutCoordinates) -> Unit,
    onDatePlaced: (LayoutCoordinates) -> Unit,
) {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L - value % 60_000L)
        }
    }
    val is24Hour = DateFormat.is24HourFormat(context)
    val time = remember(now, is24Hour) {
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", Locale.getDefault()).format(Date(now))
    }
    val date = remember(now) { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(now)) }

    Column(Modifier.padding(start = SideMargin, end = SideMargin, top = if (showClock) 28.dp else 16.dp)) {
        if (showClock) {
            Text(
                text = time,
                style = Ui.homeApp.copy(fontSize = 66.sp),
                modifier = Modifier.onGloballyPositioned(onClockPlaced),
            )
        }
        Text(
            text = date,
            style = Ui.homeApp.copy(fontSize = 18.sp),
            modifier = Modifier
                .padding(start = 3.dp)
                .onGloballyPositioned(onDatePlaced),
        )
    }
}

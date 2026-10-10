package io.github.salihoguz.minlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salihoguz.minlauncher.domain.Folders
import kotlin.math.exp
import kotlin.math.roundToInt

private val RailWidth = 28.dp

/**
 * A–Z down the right edge. Slide a finger along it: the letters near the finger bend away
 * (after Indistract) and [onLetter] reports the letter under it.
 */
@Composable
fun AlphabetRail(available: Set<Char>, onLetter: (Char) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val report by rememberUpdatedState(onLetter)
    val letters = Folders.RAIL_LETTERS
    var height by remember { mutableIntStateOf(1) }
    var touching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var current by remember { mutableStateOf<Char?>(null) }

    Box(
        modifier
            .width(RailWidth)
            .fillMaxHeight()
            .onSizeChanged { height = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    fun track(y: Float) {
                        touchY = y.coerceIn(0f, height.toFloat())
                        val index = (touchY / height * letters.size).toInt().coerceIn(0, letters.lastIndex)
                        val letter = letters[index]
                        if (letter != current) {
                            current = letter
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            report(letter)
                        }
                    }
                    touching = true
                    track(down.position.y)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        track(change.position.y)
                    }
                    touching = false
                    current = null
                }
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight(),
        ) {
            letters.forEachIndexed { index, letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 11.sp,
                    color = when {
                        letter == current -> Ui.text
                        letter in available -> Ui.dim
                        else -> Ui.text.copy(alpha = 0.2f)
                    },
                    modifier = Modifier.graphicsLayer {
                        // Read in the draw phase, so sliding doesn't recompose every letter.
                        val slot = height / letters.size.toFloat()
                        val distance = (touchY - slot * (index + 0.5f)) / slot
                        val pull = if (touching) exp(-(distance * distance) / 5f) else 0f
                        translationX = -pull * 34.dp.toPx()
                        scaleX = 1f + pull * 0.7f
                        scaleY = 1f + pull * 0.7f
                    },
                )
            }
        }
        current?.let { letter ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(-76.dp.roundToPx(), (touchY - 26.dp.toPx()).roundToInt()) }
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF2F0F6)),
            ) {
                Text(letter.toString(), color = Color(0xFF1B1A22), fontSize = 24.sp)
            }
        }
    }
}

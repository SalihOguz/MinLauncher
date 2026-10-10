package io.github.salihoguz.minlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.salihoguz.minlauncher.data.IconCache
import io.github.salihoguz.minlauncher.data.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val IconSize = 26.dp
private val glyphColor = Ui.text.copy(alpha = 0.82f)

/** The app's one-colour icon (or full colour with [color]); blank until it has loaded. */
@Composable
fun AppIcon(app: InstalledApp, modifier: Modifier = Modifier, color: Boolean = false, size: Dp = IconSize) {
    val context = LocalContext.current.applicationContext
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState(initialValue = IconCache.peek(app.key, color), app.key, color) {
        if (value == null) value = withContext(Dispatchers.IO) { IconCache.load(context, app, sizePx, color) }
    }
    Box(modifier.size(size)) {
        icon?.let {
            Image(bitmap = it, contentDescription = null, alpha = if (color) 1f else 0.85f, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Four small squares: Library, Stats and Settings. */
@Composable
fun ManageGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier.size(IconSize)) {
        val u = size.width / 24f
        val stroke = Stroke(width = 1.6f * u)
        for (x in listOf(4f, 13f)) {
            for (y in listOf(4f, 13f)) {
                drawRoundRect(
                    color = glyphColor,
                    topLeft = Offset(x * u, y * u),
                    size = Size(7f * u, 7f * u),
                    cornerRadius = CornerRadius(1.6f * u),
                    style = stroke,
                )
            }
        }
    }
}

/** A folder outline, drawn to sit with the app icons. */
@Composable
fun FolderGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier.size(IconSize)) {
        val u = size.width / 24f
        val stroke = Stroke(width = 1.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(3f * u, 8f * u)
            lineTo(3f * u, 5f * u)
            lineTo(9f * u, 5f * u)
            lineTo(11f * u, 7f * u)
        }
        drawPath(path, glyphColor, style = stroke)
        drawRoundRect(
            color = glyphColor,
            topLeft = Offset(3f * u, 7f * u),
            size = Size(18f * u, 12f * u),
            cornerRadius = CornerRadius(2f * u),
            style = stroke,
        )
    }
}

/** A plus sign, for "New folder". */
@Composable
fun PlusGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier.size(IconSize)) {
        val u = size.width / 24f
        val stroke = 1.6f * u
        drawLine(glyphColor, Offset(12f * u, 6f * u), Offset(12f * u, 18f * u), stroke, StrokeCap.Round)
        drawLine(glyphColor, Offset(6f * u, 12f * u), Offset(18f * u, 12f * u), stroke, StrokeCap.Round)
    }
}

/** A magnifying glass, for the web search row. */
@Composable
fun SearchGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier.size(IconSize)) {
        val u = size.width / 24f
        val stroke = Stroke(width = 1.6f * u, cap = StrokeCap.Round)
        drawCircle(glyphColor, radius = 6.5f * u, center = Offset(10.5f * u, 10.5f * u), style = stroke)
        drawLine(glyphColor, Offset(15.5f * u, 15.5f * u), Offset(20f * u, 20f * u), 1.6f * u, StrokeCap.Round)
    }
}

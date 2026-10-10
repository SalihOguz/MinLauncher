package io.github.salihoguz.minlauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Ui {
    val text = Color.White
    val dim = Color.White.copy(alpha = 0.66f)
    val faint = Color.White.copy(alpha = 0.42f)
    val scrim = Color(0xA80E0D18)
    val sheet = Color(0xFF1D1E26)
    val accent = Color(0xFFC9568F)
    /** An app being dragged. */
    val highlight = Color(0xFFFFD9EC)

    /** Olauncher's app name style: sans-serif-light, white, soft shadow. */
    val homeApp = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        color = text,
        shadow = Shadow(color = Color(0x55000000), offset = Offset(0f, 2f), blurRadius = 4f),
    )

    val drawerApp = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        color = text,
        fontSize = 26.sp,
        shadow = Shadow(color = Color(0x55000000), offset = Offset(0f, 2f), blurRadius = 4f),
    )

    val label = TextStyle(fontSize = 11.sp, letterSpacing = 1.5.sp, color = faint)
}

@Composable
fun MinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ui.accent,
            surface = Ui.sheet,
            background = Color.Black,
            surfaceContainerLow = Ui.sheet,
            surfaceContainerHigh = Color(0xFF2A2B35),
        ),
        content = content,
    )
}

package io.github.salihoguz.minlauncher.ui

import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salihoguz.minlauncher.data.LauncherApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SheetAction { OPEN, APP_INFO, UNINSTALL, TOGGLE_PIN, TOGGLE_HOME, RENAME, TOGGLE_HIDDEN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    app: LauncherApp,
    onHome: Boolean,
    pinned: Boolean,
    hidden: Boolean,
    onDismiss: () -> Unit,
    onAction: (SheetAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val installed = remember(app.app.installedAt) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(app.app.installedAt))
    }
    val details = buildList {
        add("Installed $installed")
        if (app.isRenamed) add("named “${app.app.label}”")
        if (app.app.user != Process.myUserHandle()) add("work profile")
        if (onHome) add("on home") else if (pinned) add("pinned")
    }.joinToString(" · ")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Ui.sheet,
        contentColor = Ui.text,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(app.label, style = Ui.drawerApp.copy(fontSize = 26.sp))
            Text(details, color = Ui.dim, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                SheetButton("Open", primary = true) { onAction(SheetAction.OPEN) }
                SheetButton("App info") { onAction(SheetAction.APP_INFO) }
                SheetButton("Uninstall", danger = true) { onAction(SheetAction.UNINSTALL) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetButton(if (pinned) "Unpin from drawer" else "Pin in drawer") { onAction(SheetAction.TOGGLE_PIN) }
                SheetButton(if (onHome) "Remove from home" else "Add to home") { onAction(SheetAction.TOGGLE_HOME) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetButton("Rename") { onAction(SheetAction.RENAME) }
                SheetButton(if (hidden) "Unhide" else "Hide") { onAction(SheetAction.TOGGLE_HIDDEN) }
            }
        }
    }
}

@Composable
private fun RowScope.SheetButton(
    text: String,
    primary: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (primary) Color(0xFFF2F0F6) else Ui.text.copy(alpha = 0.12f)
    val foreground = when {
        primary -> Color(0xFF1B1A22)
        danger -> Color(0xFFFFB4C6)
        else -> Ui.text
    }
    Text(
        text = text,
        color = foreground,
        fontSize = 14.sp,
        fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
    )
}

@Composable
fun RenameDialog(app: LauncherApp, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var text by remember { mutableStateOf(app.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Text("Original name: ${app.app.label}", color = Ui.dim, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = text.trim()
                onSave(trimmed.takeIf { it.isNotEmpty() && it != app.app.label })
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (app.isRenamed) TextButton(onClick = { onSave(null) }) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

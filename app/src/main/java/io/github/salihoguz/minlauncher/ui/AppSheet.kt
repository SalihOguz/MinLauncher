package io.github.salihoguz.minlauncher.ui

import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salihoguz.minlauncher.data.LauncherApp
import io.github.salihoguz.minlauncher.domain.Folders
import io.github.salihoguz.minlauncher.domain.Sections
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SheetAction {
    OPEN, APP_INFO, UNINSTALL, TOGGLE_PIN, TOGGLE_HOME, RENAME, TOGGLE_HIDDEN,
    REMOVE_FROM_NEW, REMOVE_FROM_RECENT, TOGGLE_SHOW_IN_RECENT,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppSheet(
    app: LauncherApp,
    onHome: Boolean,
    pinned: Boolean,
    hidden: Boolean,
    inNew: Boolean,
    inRecent: Boolean,
    folders: List<String>,
    onDismiss: () -> Unit,
    onAction: (SheetAction) -> Unit,
    onMoveToFolder: (String?) -> Unit,
    onNewFolder: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    val suggestion = if (app.folder == null) Folders.suggestion(app.app.category, folders) else null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val installed = remember(app.app.installedAt) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(app.app.installedAt))
    }
    val lastOpened = app.lastOpened
    val opened = remember(lastOpened) {
        if (lastOpened == null) "not opened from MinLauncher yet" else "opened ${relativeDays(lastOpened)}"
    }
    val details = buildList {
        add("Installed $installed")
        add(opened)
        if (app.isRenamed) add("named “${app.app.label}”")
        if (app.app.user != Process.myUserHandle()) add("work profile")
        if (onHome) add("on home") else if (pinned) add("pinned")
        add(app.folder?.let { "in $it" } ?: "Unsorted")
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
            if (suggestion != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ui.highlight.copy(alpha = 0.1f))
                        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Text(
                        "Android lists this as ${Folders.categoryName(app.app.category)}. Move to $suggestion?",
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Chip("Move", selected = true) { onMoveToFolder(suggestion) }
                }
            }
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
                SheetButton("Rename & search words") { onAction(SheetAction.RENAME) }
                SheetButton(if (hidden) "Unhide" else "Hide") { onAction(SheetAction.TOGGLE_HIDDEN) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetButton(if (picking) "Choose a folder" else "Move to folder") { picking = !picking }
            }
            if (picking) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    folders.forEach { name ->
                        Chip(name, selected = name == app.folder) { onMoveToFolder(name) }
                    }
                    Chip(Folders.UNSORTED, selected = app.folder == null) { onMoveToFolder(null) }
                    Chip("+ New folder", selected = false, onClick = onNewFolder)
                }
            }
            if (inNew || inRecent) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (inNew) SheetButton("Remove from New") { onAction(SheetAction.REMOVE_FROM_NEW) }
                    if (inRecent) SheetButton("Remove from Recent") { onAction(SheetAction.REMOVE_FROM_RECENT) }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(SheetAction.TOGGLE_SHOW_IN_RECENT) }
                    .padding(vertical = 4.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Show in Recent", fontSize = 15.sp)
                    if (app.app.isGame) Text("Off by default for games", color = Ui.faint, fontSize = 12.sp)
                }
                Switch(
                    checked = app.showInRecent,
                    onCheckedChange = { onAction(SheetAction.TOGGLE_SHOW_IN_RECENT) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Ui.accent),
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color(0xFF1B1A22) else Ui.text,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFF2F0F6) else Ui.text.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Name a new folder, or rename one. */
@Composable
fun FolderNameDialog(
    title: String,
    initial: String,
    existing: List<String>,
    renaming: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val problem = Folders.problemWithName(name, existing, renaming)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace('\n', ' ').take(Folders.MAX_NAME_LENGTH) },
                    singleLine = true,
                    label = { Text("Folder name") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                if (problem != null && name.isNotBlank()) Text(problem, color = Ui.dim, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = problem == null, onClick = { onSave(name.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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

/** "today", "yesterday", "5 days ago". */
fun relativeDays(time: Long, now: Long = System.currentTimeMillis()): String {
    val days = ((now - time) / Sections.DAY_MS).toInt()
    return when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        else -> "$days days ago"
    }
}

@Composable
fun RenameDialog(app: LauncherApp, onDismiss: () -> Unit, onSave: (label: String?, words: List<String>) -> Unit) {
    var name by remember { mutableStateOf(app.label) }
    var words by remember { mutableStateOf(app.searchWords.joinToString(", ")) }
    val wordList = { words.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename & search words") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Text("Original name: ${app.app.label}", color = Ui.dim, fontSize = 13.sp)
                OutlinedTextField(
                    value = words,
                    onValueChange = { words = it },
                    singleLine = true,
                    label = { Text("Search words") },
                    placeholder = { Text("bank, money") },
                )
                Text("Separate with commas. Search finds the app by these too.", color = Ui.dim, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                onSave(trimmed.takeIf { it.isNotEmpty() && it != app.app.label }, wordList())
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (app.isRenamed) TextButton(onClick = { onSave(null, wordList()) }) { Text("Reset name") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

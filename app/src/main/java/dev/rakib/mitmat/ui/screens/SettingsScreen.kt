package dev.rakib.mitmat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rakib.mitmat.R
import dev.rakib.mitmat.data.entity.GroupEntity
import dev.rakib.mitmat.data.preferences.AppearanceMode
import dev.rakib.mitmat.data.preferences.AppPreferences
import dev.rakib.mitmat.ui.components.ConfirmDeleteDialog
import dev.rakib.mitmat.ui.theme.AmoledCard
import dev.rakib.mitmat.ui.theme.DarkPeachCard
import dev.rakib.mitmat.ui.theme.LavenderAccent
import dev.rakib.mitmat.ui.theme.LocalIsDarkTheme
import dev.rakib.mitmat.ui.theme.MitmatCard
import dev.rakib.mitmat.ui.theme.PastelLavender
import dev.rakib.mitmat.ui.theme.PastelPeach
import dev.rakib.mitmat.ui.theme.PastelPink
import dev.rakib.mitmat.ui.theme.PastelPowderBlue
import dev.rakib.mitmat.ui.theme.PeachAccent

enum class SettingsSubView {
    MAIN,
    ARCHIVED_LIST,
    TRASH_LIST
}

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onSetAppearanceMode: (AppearanceMode) -> Unit,
    onSetIsAmoled: (Boolean) -> Unit,
    allGroups: List<GroupEntity>,
    archivedGroups: List<GroupEntity>,
    trashedGroups: List<GroupEntity>,
    onViewGroup: (Long) -> Unit,
    onUnarchiveGroup: (Long) -> Unit,
    onRestoreGroup: (Long) -> Unit,
    onPermanentlyDeleteGroup: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    onExportData: (Set<Long>?, (String) -> Unit) -> Unit,
    onImportData: (String, (Boolean, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    var activeSubView by remember { mutableStateOf(SettingsSubView.MAIN) }

    var groupToDeletePermanently by remember { mutableStateOf<GroupEntity?>(null) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }

    // Export & Group Selection Dialog states
    var showSelectBackupGroupsDialog by remember { mutableStateOf(false) }
    val backupableGroups = remember(allGroups, archivedGroups) {
        val combined = (allGroups + archivedGroups).distinctBy { it.id }
        combined.sortedByDescending { it.createdAt }
    }
    var selectedGroupIdsForBackup by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var exportedJsonText by remember { mutableStateOf<String?>(null) }

    // Import Dialog states
    var showImportDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    // Back handler to return to MAIN Settings when viewing Archive or Trash list
    BackHandler(enabled = activeSubView != SettingsSubView.MAIN) {
        activeSubView = SettingsSubView.MAIN
    }

    // Confirm permanent deletion of single group from trash
    if (groupToDeletePermanently != null) {
        ConfirmDeleteDialog(
            title = "Delete Permanently",
            message = "Permanently delete '${groupToDeletePermanently?.name}' now? All its expenses, splits, and records will be deleted immediately and cannot be recovered.",
            confirmButtonText = "Delete Now",
            onConfirm = {
                groupToDeletePermanently?.let { onPermanentlyDeleteGroup(it.id) }
                groupToDeletePermanently = null
                Toast.makeText(context, "Group permanently deleted", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { groupToDeletePermanently = null }
        )
    }

    // Confirm empty entire trash
    if (showEmptyTrashConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Empty Trash Bin",
            message = "Are you sure you want to permanently delete all ${trashedGroups.size} groups in the trash bin? This action cannot be undone.",
            confirmButtonText = "Empty All",
            onConfirm = {
                onEmptyTrash()
                showEmptyTrashConfirmDialog = false
                Toast.makeText(context, "Trash bin emptied", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showEmptyTrashConfirmDialog = false }
        )
    }

    // Group Selection Dialog for Backup Export
    if (showSelectBackupGroupsDialog) {
        AlertDialog(
            onDismissRequest = { showSelectBackupGroupsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Download, contentDescription = null, tint = LavenderAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Groups to Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose which groups to include in the exported backup file:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Select All / Deselect All Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedGroupIdsForBackup.size} of ${backupableGroups.size} selected",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LavenderAccent
                        )
                        TextButton(
                            onClick = {
                                selectedGroupIdsForBackup = if (selectedGroupIdsForBackup.size == backupableGroups.size) {
                                    emptySet()
                                } else {
                                    backupableGroups.map { it.id }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedGroupIdsForBackup.size == backupableGroups.size) "Deselect All" else "Select All",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Group Checkbox List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(backupableGroups, key = { it.id }) { group ->
                            val isSelected = group.id in selectedGroupIdsForBackup
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) PastelLavender.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    )
                                    .clickable {
                                        selectedGroupIdsForBackup = if (isSelected) {
                                            selectedGroupIdsForBackup - group.id
                                        } else {
                                            selectedGroupIdsForBackup + group.id
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedGroupIdsForBackup = if (checked) {
                                            selectedGroupIdsForBackup + group.id
                                        } else {
                                            selectedGroupIdsForBackup - group.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = LavenderAccent,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Currency: ${group.defaultCurrency}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (group.isArchived) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• Archived",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PeachAccent,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedGroupIdsForBackup.isEmpty()) {
                            Toast.makeText(context, "Please select at least one group to backup", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showSelectBackupGroupsDialog = false
                        onExportData(selectedGroupIdsForBackup) { json ->
                            exportedJsonText = json
                        }
                    },
                    enabled = selectedGroupIdsForBackup.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent)
                ) {
                    Text("Export (${selectedGroupIdsForBackup.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectBackupGroupsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Exported JSON Dialog
    if (exportedJsonText != null) {
        AlertDialog(
            onDismissRequest = { exportedJsonText = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Download, contentDescription = null, tint = LavenderAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Data Backup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Your Mitmat backup JSON is ready. You can copy and save it anywhere safe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = exportedJsonText.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Mitmat Backup", exportedJsonText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent)
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedJsonText = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isImporting) {
                    showImportDialog = false
                    importStatusMessage = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Upload, contentDescription = null, tint = LavenderAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Backup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Paste your exported Mitmat JSON backup string below to restore your groups, expenses, and settlements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = {
                            importInputText = it
                            importStatusMessage = null
                        },
                        placeholder = { Text("Paste JSON backup data here...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                    if (importStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importStatusMessage.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = PeachAccent
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isBlank()) {
                            importStatusMessage = "Please paste a JSON backup first."
                            return@Button
                        }
                        isImporting = true
                        onImportData(importInputText) { success, message ->
                            isImporting = false
                            if (success) {
                                showImportDialog = false
                                importInputText = ""
                                importStatusMessage = null
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            } else {
                                importStatusMessage = message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent),
                    enabled = !isImporting && importInputText.isNotBlank()
                ) {
                    Text(if (isImporting) "Importing..." else "Restore Backup")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        importStatusMessage = null
                    },
                    enabled = !isImporting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // MAIN or SUB-SCREENS
    when (activeSubView) {
        SettingsSubView.ARCHIVED_LIST -> {
            ArchivedGroupsListView(
                archivedGroups = archivedGroups,
                onBack = { activeSubView = SettingsSubView.MAIN },
                onViewGroup = onViewGroup,
                onUnarchiveGroup = onUnarchiveGroup,
                modifier = modifier
            )
        }

        SettingsSubView.TRASH_LIST -> {
            TrashBinListView(
                trashedGroups = trashedGroups,
                onBack = { activeSubView = SettingsSubView.MAIN },
                onRestoreGroup = onRestoreGroup,
                onRequestDeletePermanently = { groupToDeletePermanently = it },
                onRequestEmptyTrash = { showEmptyTrashConfirmDialog = true },
                modifier = modifier
            )
        }

        SettingsSubView.MAIN -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("settings_screen"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personalize appearance, archive, trash bin & backups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Appearance Section
                item {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent
                    )
                }

                item {
                    MitmatCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Appearance Mode (System, Light, Dark)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Color Appearance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppearanceChip(
                                        label = "System",
                                        icon = Icons.Rounded.SettingsBrightness,
                                        isSelected = preferences.appearanceMode == AppearanceMode.SYSTEM,
                                        onClick = { onSetAppearanceMode(AppearanceMode.SYSTEM) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    AppearanceChip(
                                        label = "Light",
                                        icon = Icons.Rounded.LightMode,
                                        isSelected = preferences.appearanceMode == AppearanceMode.LIGHT,
                                        onClick = { onSetAppearanceMode(AppearanceMode.LIGHT) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    AppearanceChip(
                                        label = "Dark",
                                        icon = Icons.Rounded.DarkMode,
                                        isSelected = preferences.appearanceMode == AppearanceMode.DARK,
                                        onClick = { onSetAppearanceMode(AppearanceMode.DARK) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // AMOLED True Black Option: ONLY shown when Dark mode is active
                            if (isDark) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (preferences.isAmoled) AmoledCard else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.DarkMode,
                                                contentDescription = null,
                                                tint = LavenderAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "AMOLED True Black",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Deep black (#000000) background for OLED battery savings",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = preferences.isAmoled,
                                        onCheckedChange = { onSetIsAmoled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = LavenderAccent
                                        ),
                                        modifier = Modifier.testTag("amoled_toggle_switch")
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Groups Management (Archive & Trash Buttons)
                item {
                    Text(
                        text = "Groups & History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Archived Groups Button
                        SettingsNavButton(
                            title = "Archived Groups",
                            subtitle = if (archivedGroups.isEmpty()) "No archived groups" else "${archivedGroups.size} group(s) archived",
                            count = archivedGroups.size,
                            icon = Icons.Rounded.Archive,
                            iconTint = LavenderAccent,
                            onClick = { activeSubView = SettingsSubView.ARCHIVED_LIST },
                            modifier = Modifier.testTag("open_archived_groups_button")
                        )

                        // Trash Bin Button
                        SettingsNavButton(
                            title = "Trash Bin",
                            subtitle = if (trashedGroups.isEmpty()) "Trash bin is empty" else "${trashedGroups.size} group(s) • Auto-deletes in 7 days",
                            count = trashedGroups.size,
                            icon = Icons.Rounded.Delete,
                            iconTint = PeachAccent,
                            onClick = { activeSubView = SettingsSubView.TRASH_LIST },
                            modifier = Modifier.testTag("open_trash_bin_button")
                        )
                    }
                }

                // 3. Data Backup & Restore (Export & Import) Section
                item {
                    Text(
                        text = "Data Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent
                    )
                }

                item {
                    MitmatCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Backup & Transfer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Export selected groups and expense records to a portable JSON backup, or import an existing backup from another device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (backupableGroups.isEmpty()) {
                                            Toast.makeText(context, "No groups found to backup", Toast.LENGTH_SHORT).show()
                                        } else {
                                            selectedGroupIdsForBackup = backupableGroups.map { it.id }.toSet()
                                            showSelectBackupGroupsDialog = true
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_data_button")
                                ) {
                                    Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export Backup", fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        importInputText = ""
                                        importStatusMessage = null
                                        showImportDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("import_data_button")
                                ) {
                                    Icon(imageVector = Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Backup", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // 4. Contact Developer Section (Rakib)
                item {
                    Text(
                        text = "Contact Developer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent
                    )
                }

                item {
                    MitmatCard(
                        modifier = Modifier.fillMaxWidth(),
                        pastelColorLight = PastelLavender.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.rakib_profile),
                                contentDescription = "Rakib - Developer",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, LavenderAccent, CircleShape)
                                    .testTag("developer_profile_image")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Rakib",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Developer of Mitmat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { openSocialProfile(context, "instagram", "iii.rakib", "https://www.instagram.com/iii.rakib/") },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PastelPink,
                                        contentColor = Color(0xFFC13584)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("contact_instagram_button")
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_instagram),
                                        contentDescription = "Instagram",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Instagram",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF833AB4)
                                    )
                                }

                                Button(
                                    onClick = { openSocialProfile(context, "facebook", "iii.rakib", "https://www.facebook.com/iii.rakib/") },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PastelPowderBlue,
                                        contentColor = Color(0xFF1877F2)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("contact_facebook_button")
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_facebook),
                                        contentDescription = "Facebook",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Facebook",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1877F2)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. About Mitmat Section
                item {
                    Text(
                        text = "About Mitmat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent
                    )
                }

                item {
                    MitmatCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Mitmat (মিটমাট)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LavenderAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "v1.0.0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Mitmat is a simple expense-splitting app that helps you share bills, track expenses, and settle balances without awkward calculations.\n\nIt does what it says.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Split bills. Stay even.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LavenderAccent
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

/**
 * Clean navigation row button in Settings (for Archive and Trash).
 */
@Composable
fun SettingsNavButton(
    title: String,
    subtitle: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MitmatCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconTint.copy(alpha = 0.18f))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Dedicated sub-view listing all archived groups with unarchive & view capabilities.
 */
@Composable
fun ArchivedGroupsListView(
    archivedGroups: List<GroupEntity>,
    onBack: () -> Unit,
    onViewGroup: (Long) -> Unit,
    onUnarchiveGroup: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("archived_groups_list_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header with Back Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .testTag("back_from_archive_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Archived Groups (${archivedGroups.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Archived groups are kept read-only for record-keeping",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (archivedGroups.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                MitmatCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(LavenderAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = null,
                                tint = LavenderAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No archived groups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When you archive a group, it will be moved here and kept read-only for future records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(archivedGroups, key = { it.id }) { group ->
                MitmatCard(
                    modifier = Modifier.fillMaxWidth(),
                    pastelColorLight = PastelPeach.copy(alpha = 0.4f),
                    pastelColorDark = DarkPeachCard.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Currency: ${group.defaultCurrency} • Read-Only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PastelPeach)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Archived", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PeachAccent)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onViewGroup(group.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onUnarchiveGroup(group.id)
                                    Toast.makeText(context, "'${group.name}' unarchived", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unarchive", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

/**
 * Dedicated sub-view listing all trashed groups with restore & delete now capabilities.
 */
@Composable
fun TrashBinListView(
    trashedGroups: List<GroupEntity>,
    onBack: () -> Unit,
    onRestoreGroup: (Long) -> Unit,
    onRequestDeletePermanently: (GroupEntity) -> Unit,
    onRequestEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trash_bin_list_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header with Back Button and Empty Trash Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("back_from_trash_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Trash Bin (${trashedGroups.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Permanently deletes after 7 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (trashedGroups.isNotEmpty()) {
                    TextButton(
                        onClick = onRequestEmptyTrash,
                        modifier = Modifier.testTag("empty_trash_action_button")
                    ) {
                        Text("Empty Trash", color = PeachAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (trashedGroups.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                MitmatCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PeachAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = PeachAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Trash bin is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When you delete a group, it moves here for 7 days before being removed permanently. You can restore or delete it immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(trashedGroups, key = { it.id }) { group ->
                val trashedTime = group.trashedAt ?: System.currentTimeMillis()
                val elapsedDays = (System.currentTimeMillis() - trashedTime) / (24 * 60 * 60 * 1000L)
                val daysRemaining = maxOf(0L, 7 - elapsedDays)

                MitmatCard(
                    modifier = Modifier.fillMaxWidth(),
                    pastelColorLight = PastelPink.copy(alpha = 0.35f),
                    pastelColorDark = DarkPeachCard.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Currency: ${group.defaultCurrency}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PastelPink)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (daysRemaining > 0) "Auto-deletes in ${daysRemaining}d" else "Deleting soon",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PeachAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onRestoreGroup(group.id)
                                    Toast.makeText(context, "'${group.name}' restored to active groups", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onRequestDeletePermanently(group) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PeachAccent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Now", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun AppearanceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PastelLavender,
            selectedLabelColor = LavenderAccent
        )
    )
}

fun openSocialProfile(context: Context, platform: String, username: String, webUrl: String) {
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
    try {
        val appIntent = when (platform) {
            "instagram" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/$username")).apply {
                    setPackage("com.instagram.android")
                }
            }
            "facebook" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=$webUrl")).apply {
                    setPackage("com.facebook.katana")
                }
            }
            else -> webIntent
        }
        context.startActivity(appIntent)
    } catch (_: Exception) {
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}

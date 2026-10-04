package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CsvBackupFolder
import com.example.data.model.HapticPreferences
import kotlin.math.roundToInt

@Composable
fun HapticFeedbackSettings(preferences: HapticPreferences?, onEnabledChange: (Boolean) -> Unit, onStrengthChange: (Int) -> Unit) {
    val feedback = LocalHapticFeedback.current
    var strength by remember(preferences?.strength) { mutableFloatStateOf((preferences?.strength ?: 50).toFloat()) }
    val enabled = preferences?.enabled == true
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Vibration, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Haptic feedback", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text("Keypad, PIN and touch gestures", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange, enabled = preferences != null,
                    modifier = Modifier.semantics { contentDescription = "Haptic feedback" })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Strength", style = MaterialTheme.typography.bodyMedium)
                Text("${strength.roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(value = strength, onValueChange = { strength = it }, valueRange = 0f..100f, steps = 19,
                onValueChangeFinished = { onStrengthChange(strength.roundToInt()) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Haptic strength" })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("0% is silent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    enabled = enabled && strength > 0) { Text("Test vibration") }
            }
        }
    }
}

@Composable
fun CsvFolderBackupControls(folder: CsvBackupFolder?, saving: Boolean, onChooseFolder: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("CSV backup to device folder", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(folder?.let { "Folder: ${it.name}" } ?: "Choose where your CSV backups are saved",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Saves all transactions to a new spreadsheet each time. Use JSON for a complete app backup.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onChooseFolder, enabled = !saving, modifier = Modifier.weight(1f)) {
                Text(if (folder == null) "Choose folder" else "Change folder")
            }
            Button(onClick = onSave, enabled = folder != null && !saving, modifier = Modifier.weight(1f)) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (saving) "Saving…" else "Back up CSV")
            }
        }
    }
}

package com.itl.wprimeext.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalInspectionMode
import com.itl.wprimeext.extension.AlertType
import com.itl.wprimeext.extension.WPrimeAlert

@Composable
fun AlertItem(
    alert: WPrimeAlert,
    onUpdate: (Int, Boolean, AlertType) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
) {
    var threshold by remember { mutableFloatStateOf(alert.thresholdPercentage.toFloat()) }
    var soundEnabled by remember { mutableStateOf(alert.soundEnabled) }
    var alertType by remember { mutableStateOf(alert.alertType) }

    val cardColor = when (alertType) {
        AlertType.REPLENISH -> Color(0xFF69A66B)

        // dark green
        AlertType.DROP -> when {
            threshold.toInt() <= 10 -> MaterialTheme.colorScheme.errorContainer
            threshold.toInt() <= 25 -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (alertType == AlertType.DROP) {
                        "Alert when W' Drops to ${threshold.toInt()}%"
                    } else {
                        "Alert when W' Recovers to ${threshold.toInt()}%"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row {
                    IconButton(onClick = onTest) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Test alert",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete alert",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alert type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = alertType == AlertType.DROP,
                    onClick = {
                        alertType = AlertType.DROP
                        onUpdate(threshold.toInt(), soundEnabled, AlertType.DROP)
                    },
                    label = { Text("↓ Drop") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                )
                FilterChip(
                    selected = alertType == AlertType.REPLENISH,
                    onClick = {
                        alertType = AlertType.REPLENISH
                        onUpdate(threshold.toInt(), soundEnabled, AlertType.REPLENISH)
                    },
                    label = { Text("↑ Recovers") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50),
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Threshold slider
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Threshold:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(56.dp),
                )
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    onValueChangeFinished = { onUpdate(threshold.toInt(), soundEnabled, alertType) },
                    valueRange = 0f..100f,
                    steps = 20,
                    modifier = Modifier.weight(2f).height(22.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.inversePrimary,
                    ),
                    )
                Text(
                    text = "${threshold.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sound toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (soundEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Sound Alert", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        onUpdate(threshold.toInt(), soundEnabled, alertType)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    ),
                )
            }
        }
    }
}

@Composable
fun NewAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Boolean, AlertType) -> Unit,
) {
    var threshold by remember { mutableFloatStateOf(25f) }
    var soundEnabled by remember { mutableStateOf(true) }
    var alertType by remember { mutableStateOf(AlertType.DROP) }

    // In Android Studio preview / inspection mode AlertDialogs are not rendered.
    // Detect preview mode and show the dialog content inline so previews are visible.
    val isInPreview = LocalInspectionMode.current

    if (isInPreview) {
        // Render the same content as the dialog inside a Card so previews show it
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Title
                Text(
                    text = "New W' Alert",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Instruction text immediately below title (per request)
                Text(
                    text = "Alert when W'",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Type selector
                Row( modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilterChip(
                        selected = alertType == AlertType.DROP,
                        onClick = { alertType = AlertType.DROP },
                        label = { Text("↓ Drops") },
                        Modifier.width(120.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = alertType == AlertType.REPLENISH,
                        onClick = { alertType = AlertType.REPLENISH },
                        label = { Text("↑ Recovers") },
                        Modifier.width(120.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF69A66B),
                            containerColor = Color(0xFF69A66B).copy(alpha = 0.75f)
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "To:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${threshold.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {threshold--} ) { Text("-") }
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 0f..100f,
                        steps = 20,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.inversePrimary,
                        ),
                    )
                    Button(onClick = {threshold++} ) { Text("+") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enable sound")
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.TextButton(onClick = { onConfirm(threshold.toInt(), soundEnabled, alertType) }) { Text("Add") }
                }
            }
        }
        return
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New W' Alert") },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(horizontal = 8.dp),
        text = {
            Column {
                // Instruction text immediately below title (per request)
                Text(
                    text = "Alert when W' drops to:",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Type selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = alertType == AlertType.DROP,
                        onClick = { alertType = AlertType.DROP },
                        label = { Text("↓ Drops") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = alertType == AlertType.REPLENISH,
                        onClick = { alertType = AlertType.REPLENISH },
                        label = { Text("↑ Recovers") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF69A66B),
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {threshold--} ) { Text("-") }
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 0f..100f,
                        steps = 20,
                        modifier = Modifier.weight(2f),
                    )
                    Button(onClick = {threshold++} ) { Text("+") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enable sound")
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(threshold.toInt(), soundEnabled, alertType) },
            ) { Text("Add") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}



@Preview(showBackground = false, heightDp = 480, widthDp = 360)
@Composable
fun PreviewNewAlertDialog() {
    NewAlertDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
}

@Preview(showBackground = true)
@Composable
fun PreviewAlertItem() {
    val sample = WPrimeAlert(id = "preview", thresholdPercentage = 25, soundEnabled = true, alertType = AlertType.DROP)
    AlertItem(alert = sample, onUpdate = { _, _, _ -> }, onDelete = {}, onTest = {})
}

@Preview(showBackground = true)
@Composable
fun PreviewAlertItemPlus() {
    val sample = WPrimeAlert(id = "preview", thresholdPercentage = 25, soundEnabled = true, alertType = AlertType.REPLENISH)
    AlertItem(alert = sample, onUpdate = { _, _, _ -> }, onDelete = {}, onTest = {})
}


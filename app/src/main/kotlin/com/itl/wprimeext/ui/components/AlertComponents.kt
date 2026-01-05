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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itl.wprimeext.extension.WPrimeAlert

@Composable
fun AlertItem(
    alert: WPrimeAlert,
    onUpdate: (Int, Boolean) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
) {
    var threshold by remember { mutableFloatStateOf(alert.thresholdPercentage.toFloat()) }
    var soundEnabled by remember { mutableStateOf(alert.soundEnabled) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                alert.thresholdPercentage <= 10 -> MaterialTheme.colorScheme.errorContainer
                alert.thresholdPercentage <= 25 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alert at ${threshold.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row {
                    IconButton(onClick = onTest) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Test alert",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete alert",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Threshold slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Threshold:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    onValueChangeFinished = {
                        onUpdate(threshold.toInt(), soundEnabled)
                    },
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${threshold.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sound toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (soundEnabled) "Sound enabled" else "Sound disabled"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sound Alert",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        onUpdate(threshold.toInt(), soundEnabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
                )
            }
        }
    }
}

@Composable
fun NewAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Boolean) -> Unit,
) {
    var threshold by remember { mutableFloatStateOf(25f) }
    var soundEnabled by remember { mutableStateOf(true) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add W' Alert") },
        text = {
            Column {
                Text(
                    text = "Alert when W' drops to:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 0f..100f,
                        steps = 99,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${threshold.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable sound")
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(threshold.toInt(), soundEnabled) }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


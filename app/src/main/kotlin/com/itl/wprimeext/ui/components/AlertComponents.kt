package com.itl.wprimeext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.itl.wprimeext.extension.AlertType
import com.itl.wprimeext.extension.WPrimeAlert
import kotlin.math.roundToInt

private val RecoverColor = Color(0xFF4CAF50)
private val DropColor = Color(0xCDFA4444)
private const val ThresholdStep = 5f
private const val Karoo3WidthDp = 256
private const val Karoo3HeightDp = 427

private fun snapThreshold(value: Float): Float = (value / ThresholdStep).roundToInt().times(ThresholdStep).coerceIn(0f, 100f)

private fun thresholdPercentage(value: Float): Int = snapThreshold(value).roundToInt()

@Composable
private fun AlertTypeSelector(
    alertType: AlertType,
    onAlertTypeChange: (AlertType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AlertTypeChip(
            label = "Drops",
            arrow = "↓",
            selected = alertType == AlertType.DROP,
            selectedColor = DropColor,
            selectedContentColor = Color.White,
            onClick = { onAlertTypeChange(AlertType.DROP) },
            modifier = Modifier.weight(1f),
        )
        AlertTypeChip(
            label = "Recovers",
            arrow = "↑",
            selected = alertType == AlertType.REPLENISH,
            selectedColor = RecoverColor,
            selectedContentColor = Color.Black,
            onClick = { onAlertTypeChange(AlertType.REPLENISH) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AlertTypeChip(
    label: String,
    arrow: String,
    selected: Boolean,
    selectedColor: Color,
    selectedContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) selectedColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
        contentColor = contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(12.dp)) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Text(
                text = "$arrow $label",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
private fun ThresholdSelector(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    onThresholdChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Threshold:", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${thresholdPercentage(threshold)}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ThresholdStepButton(
                isIncrease = false,
                enabled = threshold > 0f,
                onClick = {
                    val updatedThreshold = snapThreshold(threshold - ThresholdStep)
                    onThresholdChange(updatedThreshold)
                    onThresholdChangeFinished(updatedThreshold)
                },
            )
            Slider(
                value = threshold,
                onValueChange = { onThresholdChange(snapThreshold(it)) },
                onValueChangeFinished = { onThresholdChangeFinished(snapThreshold(threshold)) },
                valueRange = 0f..100f,
                steps = 19,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.inversePrimary,
                ),
            )
            ThresholdStepButton(
                isIncrease = true,
                enabled = threshold < 100f,
                onClick = {
                    val updatedThreshold = snapThreshold(threshold + ThresholdStep)
                    onThresholdChange(updatedThreshold)
                    onThresholdChangeFinished(updatedThreshold)
                },
            )
        }
    }
}

@Composable
private fun ThresholdStepButton(
    isIncrease: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        OutlinedIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(16.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = if (isIncrease) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = if (isIncrease) "Increase threshold" else "Decrease threshold",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun AlertItem(
    alert: WPrimeAlert,
    onUpdate: (Int, Boolean, AlertType) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
) {
    var threshold by remember(alert.id) { mutableFloatStateOf(alert.thresholdPercentage.toFloat()) }
    var soundEnabled by remember(alert.id) { mutableStateOf(alert.soundEnabled) }
    var alertType by remember(alert.id) { mutableStateOf(alert.alertType) }

    val cardColor = when (alertType) {
        AlertType.REPLENISH -> Color(0xFF69A66B)
        AlertType.DROP -> DropColor.copy(alpha = 0.18f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Alert when W'",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
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

            Spacer(modifier = Modifier.height(6.dp))

            AlertTypeSelector(
                alertType = alertType,
                onAlertTypeChange = {
                    alertType = it
                    onUpdate(thresholdPercentage(threshold), soundEnabled, it)
                },
            )

            Spacer(modifier = Modifier.height(6.dp))

            ThresholdSelector(
                threshold = threshold,
                onThresholdChange = { threshold = it },
                onThresholdChangeFinished = {
                    threshold = it
                    onUpdate(thresholdPercentage(it), soundEnabled, alertType)
                },
            )

            Spacer(modifier = Modifier.height(6.dp))

            SoundToggle(
                soundEnabled = soundEnabled,
                onSoundEnabledChange = {
                    soundEnabled = it
                    onUpdate(thresholdPercentage(threshold), it, alertType)
                },
            )
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

    val content: @Composable () -> Unit = {
        AlertEditorControls(
            threshold = threshold,
            soundEnabled = soundEnabled,
            alertType = alertType,
            onThresholdChange = { threshold = it },
            onSoundEnabledChange = { soundEnabled = it },
            onAlertTypeChange = { alertType = it },
        )
    }

    if (LocalInspectionMode.current) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "New W' Alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                content()
                Spacer(modifier = Modifier.height(8.dp))
                DialogActions(
                    onDismiss = onDismiss,
                    onConfirm = { onConfirm(thresholdPercentage(threshold), soundEnabled, alertType) },
                )
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New W' Alert", style = MaterialTheme.typography.titleMedium) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(horizontal = 8.dp),
        text = content,
        confirmButton = {
            TextButton(onClick = { onConfirm(thresholdPercentage(threshold), soundEnabled, alertType) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AlertEditorControls(
    threshold: Float,
    soundEnabled: Boolean,
    alertType: AlertType,
    onThresholdChange: (Float) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onAlertTypeChange: (AlertType) -> Unit,
) {
    Column {
        Text(text = "Alert when W'", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(6.dp))
        AlertTypeSelector(alertType = alertType, onAlertTypeChange = onAlertTypeChange)
        Spacer(modifier = Modifier.height(8.dp))
        ThresholdSelector(
            threshold = threshold,
            onThresholdChange = onThresholdChange,
            onThresholdChangeFinished = onThresholdChange,
        )
        Spacer(modifier = Modifier.height(10.dp))
        SoundToggle(
            soundEnabled = soundEnabled,
            onSoundEnabledChange = onSoundEnabledChange,
        )
    }
}

@Composable
private fun SoundToggle(
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
) {
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
            Text(text = "Sound Alert", style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = soundEnabled,
            onCheckedChange = onSoundEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            ),
        )
    }
}

@Composable
private fun DialogActions(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onConfirm) {
            Text("Add")
        }
    }
}

@Preview(showBackground = false, heightDp = Karoo3HeightDp, widthDp = Karoo3WidthDp)
@Composable
fun PreviewNewAlertDialog() {
    NewAlertDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
}

@Preview(showBackground = true, heightDp = Karoo3HeightDp, widthDp = Karoo3WidthDp)
@Composable
fun PreviewAlertItem() {
    val sample = WPrimeAlert(id = "preview", thresholdPercentage = 25, soundEnabled = true, alertType = AlertType.DROP)
    Box(modifier = Modifier.padding(16.dp)) {
        AlertItem(alert = sample, onUpdate = { _, _, _ -> }, onDelete = {}, onTest = {})
    }
}

@Preview(showBackground = true, heightDp = Karoo3HeightDp, widthDp = Karoo3WidthDp)
@Composable
fun PreviewAlertItemPlus() {
    val sample = WPrimeAlert(id = "preview-plus", thresholdPercentage = 25, soundEnabled = true, alertType = AlertType.REPLENISH)
    Box(modifier = Modifier.padding(16.dp)) {
        AlertItem(alert = sample, onUpdate = { _, _, _ -> }, onDelete = {}, onTest = {})
    }
}

package com.itl.wprimeext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itl.wprimeext.extension.AlertType
import com.itl.wprimeext.extension.WPrimeConfiguration
import com.itl.wprimeext.extension.WPrimeModelType
import com.itl.wprimeext.extension.WPrimeSettings
import com.itl.wprimeext.ui.components.AlertItem
import com.itl.wprimeext.ui.components.CompactSettingField
import com.itl.wprimeext.ui.components.NewAlertDialog
import com.itl.wprimeext.ui.theme.WPrimeExtensionTheme
import com.itl.wprimeext.ui.viewmodel.WPrimeConfigViewModel
import com.itl.wprimeext.ui.viewmodel.WPrimeConfigViewModelFactory

/**
 * Stateful composable that provides the ViewModel and state to the stateless layout.
 */
@Composable
fun ConfigurationScreen() {
    val context = LocalContext.current
    val wPrimeSettings = WPrimeSettings(context)
    val viewModel: WPrimeConfigViewModel = viewModel(
        factory = WPrimeConfigViewModelFactory(wPrimeSettings),
    )

    val configuration by viewModel.configuration.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    ConfigurationScreenLayout(
        isLoading = isLoading,
        configuration = configuration,
        onCriticalPowerChange = viewModel::updateCriticalPower,
        onAnaerobicCapacityChange = viewModel::updateAnaerobicCapacity,
        onTauRecoveryChange = viewModel::updateTauRecovery,
        onKInChange = viewModel::updateKIn,
        onRecordFitChange = viewModel::updateRecordFit,
        onShowArrowChange = viewModel::updateShowArrow,
        onUseColorsChange = viewModel::updateUseColors,
        onModelSelected = viewModel::updateModelType,
        onAddAlert = viewModel::addAlert,
        onUpdateAlert = viewModel::updateAlert,        onDeleteAlert = viewModel::deleteAlert,
        onTestAlert = { alertId ->
            // Find the alert and send broadcast to test it
            val alert = configuration.alerts.find { it.id == alertId }
            if (alert != null) {
                // Send broadcast to trigger test alert
                val intent = android.content.Intent("io.hammerhead.wprime.TEST_ALERT")
                intent.putExtra("alertId", alertId)
                intent.putExtra("threshold", alert.thresholdPercentage)
                intent.putExtra("soundEnabled", alert.soundEnabled)
                context.sendBroadcast(intent)
            }
        },
        onBackClick = { (context as? MainActivity)?.finish() },
    )
}

/**
 * Stateless layout for the configuration screen. All data is provided externally,
 * making it easy to preview and test.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreenLayout(
    isLoading: Boolean,
    configuration: WPrimeConfiguration,
    onCriticalPowerChange: (Double) -> Unit,
    onAnaerobicCapacityChange: (Double) -> Unit,
    onTauRecoveryChange: (Double) -> Unit,
    onKInChange: (Double) -> Unit,
    onRecordFitChange: (Boolean) -> Unit,
    onShowArrowChange: (Boolean) -> Unit,
    onUseColorsChange: (Boolean) -> Unit,
    onModelSelected: (WPrimeModelType) -> Unit,
    onAddAlert: (Int, Boolean, AlertType) -> Unit,
    onUpdateAlert: (String, Int, Boolean, AlertType) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onTestAlert: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val requirements = getModelRequirements(configuration.modelType)
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Configuration", "Alerts")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "W Prime Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.height(48.dp)
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Tab Row
                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    // Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        when (selectedTabIndex) {
                            0 -> ConfigurationTab(
                                configuration = configuration,
                                requirements = requirements,
                                onCriticalPowerChange = onCriticalPowerChange,
                                onAnaerobicCapacityChange = onAnaerobicCapacityChange,
                                onTauRecoveryChange = onTauRecoveryChange,
                                onKInChange = onKInChange,
                                onShowArrowChange = onShowArrowChange,
                                onUseColorsChange = onUseColorsChange,
                                onRecordFitChange = onRecordFitChange,
                                onModelSelected = onModelSelected,
                            )
                            1 -> AlertsTab(
                                alerts = configuration.alerts,
                                onAddAlert = onAddAlert,
                                onUpdateAlert = onUpdateAlert,
                                onDeleteAlert = onDeleteAlert,
                                onTestAlert = onTestAlert,
                            )
                        }

                        Spacer(modifier = Modifier.height(56.dp))
                    }
                }
            }

            FloatingActionButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 0.dp, bottom = 10.dp)
                    .size(50.dp),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 25.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 25.dp,
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }
    }
}

@Composable
fun ConfigurationTab(
    configuration: WPrimeConfiguration,
    requirements: ModelParameterRequirements,
    onCriticalPowerChange: (Double) -> Unit,
    onAnaerobicCapacityChange: (Double) -> Unit,
    onTauRecoveryChange: (Double) -> Unit,
    onKInChange: (Double) -> Unit,
    onShowArrowChange: (Boolean) -> Unit,
    onUseColorsChange: (Boolean) -> Unit,
    onRecordFitChange: (Boolean) -> Unit,
    onModelSelected: (WPrimeModelType) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "W Prime Parameters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        ModelSelectionDropdown(
            selectedModel = configuration.modelType,
            onModelSelected = onModelSelected,
        )

        CompactSettingField(
            title = "Critical Power (CP)",
            value = configuration.criticalPower,
            unit = "W",
            onValueChange = onCriticalPowerChange,
        )
        CompactSettingField(
            title = "Anaerobic Capacity (W')",
            value = configuration.anaerobicCapacity,
            unit = "J",
            onValueChange = onAnaerobicCapacityChange,
        )

        // Tau Recovery - only enabled for Bartram model
        CompactSettingField(
            title = "Tau Recovery (τ)",
            description = if (requirements.usesTau) {
                "Individualized recovery time constant"
            } else {
                "Not used by this model"
            },
            value = configuration.tauRecovery,
            unit = "s",
            onValueChange = onTauRecoveryChange,
            enabled = requirements.usesTau,
        )

        // kIn - only enabled for Weigend model
        if (requirements.usesKIn) {
            CompactSettingField(
                title = "Hydraulic Rate (kIn)",
                description = "Inflow rate coefficient",
                value = configuration.kIn,
                unit = "",
                onValueChange = onKInChange,
                enabled = true,
            )
        }

        Text(
            text = "Display Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        // Toggle for Show Arrow
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                    Text(
                        text = "Show Trend Arrow",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Display arrow indicating W' trend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = configuration.showArrow,
                    onCheckedChange = onShowArrowChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    ),
                )
            }
        }

        // Toggle for Use Colors
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                    Text(
                        text = "Use Dynamic Colors",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Colorize background based on W' depletion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = configuration.useColors,
                    onCheckedChange = onUseColorsChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    ),
                )
            }
        }

        Text(
            text = "System Integration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        // Toggle para grabar datos W' al archivo FIT
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                    Text(
                        text = "Record W' to FIT",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Add W' fields to FIT file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = configuration.recordFit,
                    onCheckedChange = onRecordFitChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    ),
                )
            }
        }

        Text(
            text = "Changes saved automatically",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun AlertsTab(
    alerts: List<com.itl.wprimeext.extension.WPrimeAlert>,
    onAddAlert: (Int, Boolean, AlertType) -> Unit,
    onUpdateAlert: (String, Int, Boolean, AlertType) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onTestAlert: (String) -> Unit,
) {
    var showNewAlertDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "W' Alerts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = "Set Drop alerts when W' falls through a threshold (yellow), and Recover alerts when W' rises back through one (green).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (alerts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No alerts configured",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add alerts to get notified when W' reaches critical levels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            alerts.sortedBy { it.thresholdPercentage }.forEach { alert ->
                AlertItem(
                    alert = alert,
                    onUpdate = { threshold, sound, type ->
                        onUpdateAlert(alert.id, threshold, sound, type)
                    },
                    onDelete = { onDeleteAlert(alert.id) },
                    onTest = { onTestAlert(alert.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Button(
            onClick = { showNewAlertDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add alert"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Alert")
        }

        if (showNewAlertDialog) {
            NewAlertDialog(
                onDismiss = { showNewAlertDialog = false },
                onConfirm = { threshold, sound, type ->
                    onAddAlert(threshold, sound, type)
                    showNewAlertDialog = false
                }
            )
        }

        Text(
            text = "Each alert fires at most once every 5 min to avoid spam during repeated efforts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionDropdown(
    selectedModel: WPrimeModelType,
    onModelSelected: (WPrimeModelType) -> Unit,
) {
    val models = WPrimeModelType.entries
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextField(
                value = formatModelName(selectedModel),
                onValueChange = {},
                readOnly = true,
                label = { Text("W' Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(text = formatModelName(model)) },
                        onClick = {
                            onModelSelected(model)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun formatModelName(model: WPrimeModelType): String = when (model) {
    WPrimeModelType.SKIBA_2012 -> "Skiba 2012"
    WPrimeModelType.SKIBA_DIFFERENTIAL -> "Skiba Differential (2014)"
    WPrimeModelType.BARTRAM -> "Bartram 2018"
    WPrimeModelType.CAEN_LIEVENS -> "Caen/Lievens (Domain)"
    WPrimeModelType.CHORLEY -> "Chorley 2023 (Bi-Exp)"
    WPrimeModelType.WEIGEND -> "Weigend 2022 (Hydraulic)"
}

/**
 * Determines which configuration parameters are used by each model.
 */
data class ModelParameterRequirements(
    val usesTau: Boolean,
    val usesKIn: Boolean,
)

private fun getModelRequirements(model: WPrimeModelType): ModelParameterRequirements = when (model) {
    WPrimeModelType.SKIBA_2012,
    WPrimeModelType.SKIBA_DIFFERENTIAL,
    WPrimeModelType.CAEN_LIEVENS,
    WPrimeModelType.CHORLEY,
    -> ModelParameterRequirements(usesTau = false, usesKIn = false)

    WPrimeModelType.BARTRAM -> ModelParameterRequirements(usesTau = true, usesKIn = false)

    WPrimeModelType.WEIGEND -> ModelParameterRequirements(usesTau = false, usesKIn = true)
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
fun ConfigurationScreenPreview() {
    WPrimeExtensionTheme {
        ConfigurationScreenLayout(
            isLoading = false,
            configuration = WPrimeConfiguration(
                criticalPower = 280.0,
                anaerobicCapacity = 22000.0,
                tauRecovery = 320.0,
                kIn = 0.002,
                recordFit = true,
                modelType = WPrimeModelType.BARTRAM,
                showArrow = true,
                useColors = true,
            ),
            onCriticalPowerChange = {},
            onAnaerobicCapacityChange = {},
            onTauRecoveryChange = {},
            onKInChange = {},
            onRecordFitChange = {},
            onShowArrowChange = {},
            onUseColorsChange = {},
            onModelSelected = {},
            onAddAlert = { _, _, _ -> },
            onUpdateAlert = { _, _, _, _ -> },
            onDeleteAlert = {},
            onTestAlert = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ModelSelectionDropdownPreview() {
    WPrimeExtensionTheme {
        ModelSelectionDropdown(
            selectedModel = WPrimeModelType.SKIBA_DIFFERENTIAL,
            onModelSelected = {},
        )
    }
}

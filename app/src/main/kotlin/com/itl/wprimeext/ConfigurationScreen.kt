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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.itl.wprimeext.extension.WPrimeConfiguration
import com.itl.wprimeext.extension.WPrimeModelType
import com.itl.wprimeext.extension.WPrimeSettings
import com.itl.wprimeext.ui.components.CompactSettingField
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
        onModelSelected = viewModel::updateModelType,
        onBackClick = { (context as? MainActivity)?.finish() }
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
    onModelSelected: (WPrimeModelType) -> Unit,
    onBackClick: () -> Unit
) {
    val requirements = getModelRequirements(configuration.modelType)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("W Prime Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        text = "W Prime Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    ModelSelectionDropdown(
                        selectedModel = configuration.modelType,
                        onModelSelected = onModelSelected
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
                        description = if (requirements.usesTau) "Individualized recovery time constant"
                                     else "Not used by this model",
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

                    // Toggle para grabar datos W' al archivo FIT
                    androidx.compose.material3.Card(
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

                    Spacer(modifier = Modifier.height(56.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionDropdown(
    selectedModel: WPrimeModelType,
    onModelSelected: (WPrimeModelType) -> Unit
) {
    val models = WPrimeModelType.entries
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = formatModelName(selectedModel),
                onValueChange = {},
                readOnly = true,
                label = { Text("W' Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(text = formatModelName(model)) },
                        onClick = {
                            onModelSelected(model)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatModelName(model: WPrimeModelType): String {
    return when (model) {
        WPrimeModelType.SKIBA_2012 -> "Skiba 2012"
        WPrimeModelType.SKIBA_DIFFERENTIAL -> "Skiba Differential (2014)"
        WPrimeModelType.BARTRAM -> "Bartram 2018"
        WPrimeModelType.CAEN_LIEVENS -> "Caen/Lievens (Domain)"
        WPrimeModelType.CHORLEY -> "Chorley 2023 (Bi-Exp)"
        WPrimeModelType.WEIGEND -> "Weigend 2022 (Hydraulic)"
    }
}

/**
 * Determines which configuration parameters are used by each model.
 */
data class ModelParameterRequirements(
    val usesTau: Boolean,
    val usesKIn: Boolean
)

private fun getModelRequirements(model: WPrimeModelType): ModelParameterRequirements {
    return when (model) {
        WPrimeModelType.SKIBA_2012,
        WPrimeModelType.SKIBA_DIFFERENTIAL,
        WPrimeModelType.CAEN_LIEVENS,
        WPrimeModelType.CHORLEY -> ModelParameterRequirements(usesTau = false, usesKIn = false)

        WPrimeModelType.BARTRAM -> ModelParameterRequirements(usesTau = true, usesKIn = false)

        WPrimeModelType.WEIGEND -> ModelParameterRequirements(usesTau = false, usesKIn = true)
    }
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
                modelType = WPrimeModelType.BARTRAM
            ),
            onCriticalPowerChange = {},
            onAnaerobicCapacityChange = {},
            onTauRecoveryChange = {},
            onKInChange = {},
            onRecordFitChange = {},
            onModelSelected = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ModelSelectionDropdownPreview() {
    WPrimeExtensionTheme {
        ModelSelectionDropdown(
            selectedModel = WPrimeModelType.SKIBA_DIFFERENTIAL,
            onModelSelected = {}
        )
    }
}

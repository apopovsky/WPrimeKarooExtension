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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itl.wprimeext.extension.WPrimeConfiguration
import com.itl.wprimeext.extension.WPrimeSettings
import com.itl.wprimeext.ui.components.CompactSettingField
import com.itl.wprimeext.ui.viewmodel.WPrimeConfigViewModel
import com.itl.wprimeext.ui.viewmodel.WPrimeConfigViewModelFactory

@Composable
fun ConfigurationScreen() {
    val context = LocalContext.current
    val wPrimeSettings = WPrimeSettings(context)
    val viewModel: WPrimeConfigViewModel = viewModel(
        factory = WPrimeConfigViewModelFactory(wPrimeSettings),
    )

    val configuration by viewModel.configuration.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    ConfigurationScreenContent(
        isLoading = isLoading,
        configuration = configuration,
        onCriticalPowerChange = viewModel::updateCriticalPower,
        onAnaerobicCapacityChange = viewModel::updateAnaerobicCapacity,
        onTauRecoveryChange = viewModel::updateTauRecovery,
        onRecordFitChange = viewModel::updateRecordFit,
        onBackClick = { (context as? MainActivity)?.finish() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreenContent(
    isLoading: Boolean,
    configuration: WPrimeConfiguration,
    onCriticalPowerChange: (Double) -> Unit,
    onAnaerobicCapacityChange: (Double) -> Unit,
    onTauRecoveryChange: (Double) -> Unit,
    onRecordFitChange: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "W Prime Settings",
                        fontWeight = FontWeight.Bold,
                    )
                },
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

                    // Campos compactos
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
                    CompactSettingField(
                        title = "Tau Recovery",
                        value = configuration.tauRecovery,
                        unit = "s",
                        onValueChange = onTauRecoveryChange,
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
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(56.dp))
                }
            }

            // FAB posicionado como en ki2: esquina inferior izquierda con margen
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
                    imageVector = ArrowBack,
                    contentDescription = "Back",
                )
            }
        }
    }
}

@Preview
@Composable
fun ConfigurationScreenPreview() {
    ConfigurationScreenContent(
        isLoading = false,
        configuration = WPrimeConfiguration(
            criticalPower = 300.0,
            anaerobicCapacity = 15000.0,
            tauRecovery = 250.0,
            recordFit = true
        ),
        onCriticalPowerChange = {},
        onAnaerobicCapacityChange = {},
        onTauRecoveryChange = {},
        onRecordFitChange = {},
        onBackClick = {}
    )
}

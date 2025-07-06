package com.itl.wprimeext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itl.wprimeext.extension.WPrimeSettings
import com.itl.wprimeext.ui.components.ConfigurationCard
import com.itl.wprimeext.ui.viewmodel.WPrimeConfigViewModel
import com.itl.wprimeext.ui.viewmodel.WPrimeConfigViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen() {
    val context = LocalContext.current
    val wPrimeSettings = WPrimeSettings(context)
    val viewModel: WPrimeConfigViewModel = viewModel(
        factory = WPrimeConfigViewModelFactory(wPrimeSettings),
    )

    val configuration by viewModel.configuration.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuración W Prime",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        if (!isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Parámetros del modelo W Prime",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Configura los parámetros para el cálculo de W Prime basado en tu perfil de potencia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                ConfigurationCard(
                    title = "Potencia Crítica (CP)",
                    description = "La potencia máxima que puedes mantener en estado estable. Típicamente determinada por un test de 20 minutos multiplicado por 0.95.",
                    value = configuration.criticalPower,
                    unit = "W",
                    onValueChange = viewModel::updateCriticalPower,
                )

                ConfigurationCard(
                    title = "Capacidad Anaeróbica (W')",
                    description = "La cantidad de energía anaeróbica disponible por encima de la potencia crítica. Valor típico entre 10000-25000 julios.",
                    value = configuration.anaerobicCapacity,
                    unit = "J",
                    onValueChange = viewModel::updateAnaerobicCapacity,
                )

                ConfigurationCard(
                    title = "Constante de Recuperación (Tau)",
                    description = "La constante de tiempo que determina la velocidad de recuperación de W'. Valores típicos entre 200-600 segundos.",
                    value = configuration.tauRecovery,
                    unit = "s",
                    onValueChange = viewModel::updateTauRecovery,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "💡 Consejo: Para obtener valores precisos, considera realizar un test de laboratorio o usar datos de entrenamientos recientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

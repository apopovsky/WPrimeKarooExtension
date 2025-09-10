package com.itl.wprimeext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
                        text = "Parámetros W Prime",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ConfigurationCard(
                        title = "Potencia Crítica (CP)",
                        description = "Potencia máxima sostenible",
                        value = configuration.criticalPower,
                        unit = "W",
                        onValueChange = viewModel::updateCriticalPower,
                    )

                    ConfigurationCard(
                        title = "Capacidad Anaeróbica (W')",
                        description = "Energía disponible sobre CP",
                        value = configuration.anaerobicCapacity,
                        unit = "J",
                        onValueChange = viewModel::updateAnaerobicCapacity,
                    )

                    ConfigurationCard(
                        title = "Tau Recuperación",
                        description = "Velocidad de recuperación de W'",
                        value = configuration.tauRecovery,
                        unit = "s",
                        onValueChange = viewModel::updateTauRecovery,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "✓ Guardado automático",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // FAB posicionado como en ki2: esquina inferior izquierda con margen
            FloatingActionButton(
                onClick = {
                    (context as? MainActivity)?.finish()
                },
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
                    contentDescription = "Volver",
                )
            }
        }
    }
}

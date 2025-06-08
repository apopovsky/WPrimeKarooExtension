package com.itl.wprimeextension.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.itl.wprimeextension.data.WPrimeSettings
import com.itl.wprimeextension.theme.AppTheme

@Composable
fun MainScreen() {
    var showConfiguration by remember { mutableStateOf(false) }

    if (showConfiguration) {
        ConfigurationScreen()
    } else {
        MainScreenContent(
            onNavigateToConfiguration = { showConfiguration = true },
        )
    }
}

@Composable
private fun MainScreenContent(
    onNavigateToConfiguration: () -> Unit,
) {
    val context = LocalContext.current
    val wPrimeSettings = WPrimeSettings(context)
    val configuration by wPrimeSettings.configuration.collectAsState(initial = null)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "W Prime Extension",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Extensión para Hammerhead Karoo",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            // W Prime Status Card
            WPrimeStatusCard()

            Spacer(modifier = Modifier.height(16.dp))

            configuration?.let { config ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = "Configuración Actual",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Potencia Crítica: ${config.criticalPower.toInt()} W")
                        Text("Capacidad Anaeróbica: ${config.anaerobicCapacity.toInt()} J")
                        Text("Tau Recuperación: ${config.tauRecovery.toInt()} s")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToConfiguration,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Configurar W Prime")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Instala esta extensión en tu Hammerhead Karoo para acceder al campo de datos W Prime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(
    widthDp = 256,
    heightDp = 426,
)
@Composable
fun DefaultPreview() {
    AppTheme {
        MainScreen()
    }
}

package com.example.apptest2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apptest2.ui.theme.Apptest2Theme
import com.example.apptest2.usb.UsbCdcManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Apptest2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UsbTestScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun UsbTestScreen(modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Prêt à envoyer") }
    var isLoading by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val usbCdcManager = remember { UsbCdcManager(context) }
    val listState = rememberLazyListState()

    // Auto-scroll vers le bas quand de nouveaux logs arrivent
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val handleSend = { trame: String, trameNumber: Int ->
        if (!isLoading) {
            coroutineScope.launch {
                isLoading = true
                status = "Envoi de la Trame $trameNumber..."

                val success = usbCdcManager.sendString(trame)
                logs = usbCdcManager.logMessages

                status = if (success) {
                    "Trame $trameNumber envoyée avec succès"
                } else {
                    "Erreur lors de l'envoi de la Trame $trameNumber"
                }
                isLoading = false
            }
        }
    }

    val handleDiagnostic = {
        if (!isLoading) {
            coroutineScope.launch {
                isLoading = true
                status = "Diagnostic USB en cours..."

                val devices = usbCdcManager.listConnectedDevices()
                status = if (devices.isEmpty()) {
                    "Aucun périphérique USB détecté"
                } else {
                    "Trouvé ${devices.size} périphérique(s) USB"
                }

                // Forcer un appel pour voir les logs détaillés
                usbCdcManager.sendString("TEST_DIAGNOSTIC")
                logs = usbCdcManager.logMessages

                isLoading = false
            }
        }
    }

    val clearLogs = {
        usbCdcManager.clearLogs()
        logs = emptyList()
        status = "Logs effacés"
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        // Zone de statut
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (status.contains("Erreur")) {
                    MaterialTheme.colorScheme.errorContainer
                } else if (status.contains("succès")) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = status,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.contains("Erreur")) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons de contrôle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { handleDiagnostic() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("🔍 DIAGNOSTIC")
            }

            Button(
                onClick = { clearLogs() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("🗑️ EFFACER")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons de trames
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { handleSend("TRAME1\r\n", 1) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Trame 1")
            }

            Button(
                onClick = { handleSend("TRAME2\r\n", 2) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Trame 2")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { handleSend("TRAME3\r\n", 3) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Trame 3")
            }

            Button(
                onClick = { handleSend("TRAME4\r\n", 4) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Trame 4")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Zone des logs
        Text(
            text = "Logs USB (${logs.size} entrées):",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(8.dp)
            ) {
                items(logs) { logEntry ->
                    Text(
                        text = logEntry,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            logEntry.contains("❌") -> MaterialTheme.colorScheme.error
                            logEntry.contains("✅") -> MaterialTheme.colorScheme.primary
                            logEntry.contains("⚠️") -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UsbTestScreenPreview() {
    Apptest2Theme {
        UsbTestScreen()
    }
}

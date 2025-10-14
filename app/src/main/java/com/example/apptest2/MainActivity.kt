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
import com.example.apptest2.wristband.WristbandFrameManager
import com.example.apptest2.wristband.WristbandStyle
import com.example.apptest2.wristband.WristbandColor
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
    var wristbandInitialized by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val usbCdcManager = remember { UsbCdcManager(context) }
    val wristbandFrameManager = remember { WristbandFrameManager() }
    val listState = rememberLazyListState()

    // Initialiser le gestionnaire wristband au démarrage
    LaunchedEffect(Unit) {
        try {
            wristbandInitialized = wristbandFrameManager.initialize()
            status = if (wristbandInitialized) {
                "✅ Gestionnaire wristband initialisé avec succès"
            } else {
                "⚠️ Erreur d'initialisation du gestionnaire wristband"
            }
        } catch (e: Exception) {
            status = "❌ Erreur fatale: ${e.message}"
            wristbandInitialized = false
        }
    }

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

    val handleSendWristbandFrame = { frameType: String, data: String, frameNumber: Int ->
        if (!isLoading) {
            coroutineScope.launch {
                isLoading = true
                status = "Génération et envoi de la trame Event $frameNumber..."

                try {
                    // Générer une trame Event avec la librairie wristband_objects
                    val color = when (frameNumber) {
                        1 -> WristbandColor(red = 255, green = 0, blue = 0)      // Rouge
                        2 -> WristbandColor(red = 0, green = 255, blue = 0)      // Vert
                        3 -> WristbandColor(red = 0, green = 0, blue = 255)      // Bleu
                        4 -> WristbandColor(red = 255, green = 255, blue = 255)  // Blanc
                        else -> WristbandColor(red = 255, green = 0, blue = 0)
                    }

                    val style = when (frameType) {
                        "COMMAND" -> WristbandStyle.ON
                        "DATA" -> WristbandStyle.STROBE
                        "STATUS" -> WristbandStyle.PULSE
                        "ACK" -> WristbandStyle.HEARTBEAT
                        else -> WristbandStyle.ON
                    }

                    // Générer la trame avec Event.encode()
                    val generatedFrame = wristbandFrameManager.generateSimpleEvent(style, color)

                    // Convertir en hex pour le debug
                    val hexString = wristbandFrameManager.frameToHexString(generatedFrame)

                    // Valider la trame avant l'envoi
                    val isValid = wristbandFrameManager.validateFrame(generatedFrame)
                    if (!isValid) {
                        status = "❌ Trame $frameNumber invalide"
                        isLoading = false
                        return@launch
                    }

                    // Envoyer la trame via USB CDC
                    val success = usbCdcManager.sendBytes(generatedFrame)
                    logs = usbCdcManager.logMessages

                    status = if (success) {
                        "✅ Trame Event $frameNumber envoyée (${generatedFrame.size} octets)\n$hexString"
                    } else {
                        "❌ Erreur lors de l'envoi de la trame Event $frameNumber"
                    }
                } catch (e: Exception) {
                    status = "❌ Erreur: ${e.message}"
                }
                isLoading = false
            }
        }
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

        Spacer(modifier = Modifier.height(8.dp))

        // Nouveaux boutons pour les trames Event
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { handleSendWristbandFrame("COMMAND", "data", 1) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Event CMD")
            }

            Button(
                onClick = { handleSendWristbandFrame("DATA", "data", 2) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Event DATA")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { handleSendWristbandFrame("STATUS", "data", 3) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Event STATUS")
            }

            Button(
                onClick = { handleSendWristbandFrame("ACK", "data", 4) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Event ACK")
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

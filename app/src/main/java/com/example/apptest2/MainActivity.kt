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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.example.apptest2.ui.DefaultMessageConfigs
import com.example.apptest2.ui.MessageConfig
import com.example.apptest2.ui.MessageConfigScreen
import com.example.apptest2.usb.UsbCdcManager
import com.example.apptest2.wristband.WristbandFrameManager
import com.example.apptest2.sync.TimeSyncService
import com.example.apptest2.storage.ConfigurationStorage
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
    var isDiagnosticActive by remember { mutableStateOf(false) }
    var showConfigScreen by remember { mutableStateOf(false) }
    var selectedButtonForConfig by remember { mutableStateOf(1) }

    // États pour le service de synchronisation automatique - ACTIVÉ PAR DÉFAUT
    var timeSyncActive by remember { mutableStateOf(true) }
    var syncStats by remember { mutableStateOf("Sync: Démarrage...") }

    // État pour stocker les configurations des messages
    var messageConfigs by remember { mutableStateOf(DefaultMessageConfigs.configs.toMutableMap()) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val usbCdcManager = remember { UsbCdcManager(context) }
    val wristbandFrameManager = remember { WristbandFrameManager() }
    val listState = rememberLazyListState()

    // Service de synchronisation automatique du temps
    val timeSyncService = remember {
        TimeSyncService(context, usbCdcManager, wristbandFrameManager)
    }

    // Gestionnaire de sauvegarde/chargement des configurations
    val configurationStorage = remember { ConfigurationStorage(context) }

    // Charger les configurations sauvegardées au démarrage
    LaunchedEffect(Unit) {
        try {
            val loadedConfigs = configurationStorage.loadConfigurations()
            messageConfigs = loadedConfigs

            val hasStoredConfigs = configurationStorage.hasStoredConfigurations()
            if (hasStoredConfigs) {
                status = "📂 Configurations des boutons chargées depuis la sauvegarde"
                android.util.Log.i("MainActivity", "✅ ${loadedConfigs.size} configurations chargées depuis la sauvegarde")
            } else {
                status = "📋 Configurations par défaut chargées"
                android.util.Log.i("MainActivity", "📋 Utilisation des configurations par défaut")
            }
        } catch (e: Exception) {
            status = "⚠️ Erreur lors du chargement des configurations"
            android.util.Log.e("MainActivity", "Erreur lors du chargement des configurations", e)
        }
    }

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

    // Démarrer automatiquement la synchronisation au lancement de l'application
    LaunchedEffect(Unit) {
        try {
            timeSyncService.start()
            status = "🕐 Synchronisation automatique du temps démarrée (3005ms)"
        } catch (e: Exception) {
            status = "❌ Erreur démarrage synchronisation: ${e.message}"
            timeSyncActive = false
            android.util.Log.e("MainActivity", "Erreur démarrage synchronisation automatique", e)
        }
    }

    // Auto-scroll vers le bas quand de nouveaux logs arrivent
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    // Mise à jour périodique des statistiques de synchronisation
    LaunchedEffect(timeSyncActive) {
        while (timeSyncActive) {
            kotlinx.coroutines.delay(1000) // Mise à jour chaque seconde
            val stats = timeSyncService.getStats()
            syncStats = "Sync: ${stats.attempts} tentatives, ${stats.successes} succès (${stats.successRate}%)"
        }
    }

    // Nettoyage du service lors de la destruction de l'interface
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            timeSyncService.cleanup()
        }
    }

    // Fonction pour gérer le service de synchronisation
    val handleTimeSyncToggle = {
        if (!isLoading) {
            coroutineScope.launch {
                isLoading = true
                try {
                    if (timeSyncActive) {
                        timeSyncService.stop()
                        timeSyncActive = false
                        syncStats = "Sync: Arrêté"
                        status = "🔴 Synchronisation automatique du temps arrêtée"
                    } else {
                        timeSyncService.start()
                        timeSyncActive = true
                        status = "🕐 Synchronisation automatique du temps démarrée (3005ms)"
                    }
                } catch (e: Exception) {
                    status = "❌ Erreur service synchronisation: ${e.message}"
                    android.util.Log.e("MainActivity", "Erreur service synchronisation", e)
                }
                isLoading = false
            }
        }
    }

    // Fonction simplifiée pour envoyer uniquement des événements détaillés
    val handleSendMessage = { buttonNumber: Int ->
        if (!isLoading) {
            coroutineScope.launch {
                isLoading = true
                val config = messageConfigs[buttonNumber] ?: return@launch
                status = "Envoi de ${config.name}..."

                try {
                    // Vérifier d'abord si des périphériques USB sont disponibles
                    val devices = usbCdcManager.listConnectedDevices()
                    if (devices.isEmpty()) {
                        status = "⚠️ Aucun périphérique USB détecté - Connectez un périphérique CDC"
                        isLoading = false
                        return@launch
                    }

                    // Toujours utiliser la configuration détaillée AVEC TEMPS RELATIFS
                    val frame = wristbandFrameManager.generateDetailedEventWithRelativeTime(
                        config.detailedEventConfig,
                        timeSyncService.getApplicationStartTime()
                    )
                    val isValid = wristbandFrameManager.validateFrame(frame)

                    if (!isValid) {
                        status = "❌ Trame ${config.name} invalide"
                        isLoading = false
                        return@launch
                    }

                    status = "📡 Envoi de ${config.name} via USB..."
                    val success = usbCdcManager.sendBytes(frame)

                    // Mettre à jour les logs seulement si le diagnostic est actif
                    if (isDiagnosticActive) {
                        logs = usbCdcManager.logMessages
                    }

                    status = if (success) {
                        "✅ ${config.name} envoyé avec succès"
                    } else {
                        "❌ Erreur lors de l'envoi de ${config.name} - Vérifiez la connexion USB"
                    }
                } catch (e: Exception) {
                    status = "❌ Erreur: ${e.message}"
                    android.util.Log.e("MainActivity", "Erreur lors de l'envoi de ${config.name}", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val handleDiagnostic = {
        if (!isLoading) {
            coroutineScope.launch {
                isLoading = true

                if (!isDiagnosticActive) {
                    status = "Activation du diagnostic USB..."
                    val devices = usbCdcManager.listConnectedDevices()
                    if (devices.isEmpty()) {
                        status = "⚠️ Diagnostic activé - Aucun périphérique USB détecté"
                    } else {
                        status = "✅ Diagnostic activé - ${devices.size} périphérique(s) USB détecté(s)"
                    }
                    usbCdcManager.sendString("TEST_DIAGNOSTIC")
                    logs = usbCdcManager.logMessages
                    isDiagnosticActive = true
                } else {
                    status = "🔴 Diagnostic désactivé - Affichage des logs arrêté"
                    isDiagnosticActive = false
                }
                isLoading = false
            }
        }
    }

    val clearLogs = {
        usbCdcManager.clearLogs()
        logs = emptyList()
        status = "Logs effacés"
    }

    // Affichage conditionnel : écran principal ou écran de configuration
    if (showConfigScreen) {
        MessageConfigScreen(
            buttonNumber = selectedButtonForConfig,
            currentConfig = messageConfigs[selectedButtonForConfig] ?: MessageConfig(),
            onConfigChange = { newConfig ->
                messageConfigs = messageConfigs.toMutableMap().apply {
                    put(selectedButtonForConfig, newConfig)
                }

                // Sauvegarder automatiquement les configurations modifiées
                coroutineScope.launch {
                    try {
                        configurationStorage.saveConfigurations(messageConfigs)
                        android.util.Log.d("MainActivity", "💾 Configuration du bouton $selectedButtonForConfig sauvegardée automatiquement")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "❌ Erreur lors de la sauvegarde automatique: ${e.message}", e)
                    }
                }
            },
            onBack = { showConfigScreen = false }
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp)
        ) {
            // Zone de statut
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp),
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
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status.contains("Erreur")) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 4,
                    minLines = 4
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
                        containerColor = if (isDiagnosticActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Text(
                        if (isDiagnosticActive) {
                            "🟢 DIAGNOSTIC ON"
                        } else {
                            "🔍 DIAGNOSTIC OFF"
                        }
                    )
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

            Spacer(modifier = Modifier.height(8.dp))

            // Bouton de contrôle de la synchronisation automatique du temps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { handleTimeSyncToggle() },
                    enabled = !isLoading,
                    modifier = Modifier.weight(2f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (timeSyncActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    Text(
                        if (timeSyncActive) {
                            "🕐 SYNC ON (3005ms)"
                        } else {
                            "⏰ SYNC OFF"
                        }
                    )
                }

                // Affichage des statistiques de synchronisation
                Card(
                    modifier = Modifier.weight(3f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = syncStats,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Boutons de messages configurables (8 boutons, 2 par ligne)
            for (row in 0..3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0..1) {
                        val buttonNumber = row * 2 + col + 1
                        val config = messageConfigs[buttonNumber] ?: MessageConfig()

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { handleSendMessage(buttonNumber) },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = config.name,
                                    maxLines = 1
                                )
                            }

                            FloatingActionButton(
                                onClick = {
                                    selectedButtonForConfig = buttonNumber
                                    showConfigScreen = true
                                },
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Configurer ${config.name}",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (row < 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Zone des logs - Affichage conditionnel basé sur l'état du diagnostic
            if (isDiagnosticActive) {
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
            } else {
                // Message informatif quand les logs ne sont pas affichés
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📊",
                            fontSize = 48.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Affichage des logs désactivé",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Activez le diagnostic pour voir les logs USB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
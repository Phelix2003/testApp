package com.example.apptest2.sync

import android.content.Context
import android.util.Log
import com.example.apptest2.usb.UsbCdcManager
import com.example.apptest2.wristband.WristbandFrameManager
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service de synchronisation automatique du temps
 * Envoie un message de synchronisation du temps toutes les 3005ms
 * Gère automatiquement les connexions/déconnexions USB
 */
class TimeSyncService(
    private val context: Context,
    private val usbCdcManager: UsbCdcManager,
    private val wristbandFrameManager: WristbandFrameManager
) {
    companion object {
        private const val TAG = "TimeSyncService"
        private const val SYNC_INTERVAL_MS = 3005L // Intervalle de synchronisation en millisecondes
    }

    private val isRunning = AtomicBoolean(false)
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Compteurs pour les statistiques
    private var syncAttempts = 0
    private var syncSuccesses = 0
    private var syncFailures = 0
    private var lastSyncTime = 0L

    /**
     * Démarre le service de synchronisation automatique
     */
    fun start() {
        if (isRunning.get()) {
            Log.w(TAG, "Service de synchronisation déjà démarré")
            return
        }

        Log.i(TAG, "🚀 Démarrage du service de synchronisation automatique")
        Log.i(TAG, "Intervalle de synchronisation: ${SYNC_INTERVAL_MS}ms")

        isRunning.set(true)

        syncJob = scope.launch {
            while (isRunning.get()) {
                try {
                    performTimeSync()
                    delay(SYNC_INTERVAL_MS)
                } catch (e: CancellationException) {
                    Log.i(TAG, "Service de synchronisation annulé")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur dans la boucle de synchronisation: ${e.message}", e)
                    delay(SYNC_INTERVAL_MS) // Attendre avant de réessayer
                }
            }
        }
    }

    /**
     * Arrête le service de synchronisation automatique
     */
    fun stop() {
        if (!isRunning.get()) {
            Log.w(TAG, "Service de synchronisation déjà arrêté")
            return
        }

        Log.i(TAG, "🛑 Arrêt du service de synchronisation automatique")

        isRunning.set(false)
        syncJob?.cancel()

        // Afficher les statistiques finales
        Log.i(TAG, "📊 Statistiques de synchronisation:")
        Log.i(TAG, "  Tentatives: $syncAttempts")
        Log.i(TAG, "  Succès: $syncSuccesses")
        Log.i(TAG, "  Échecs: $syncFailures")
        if (syncAttempts > 0) {
            val successRate = (syncSuccesses * 100) / syncAttempts
            Log.i(TAG, "  Taux de succès: ${successRate}%")
        }
    }

    /**
     * Vérifie si le service est en cours d'exécution
     */
    fun isActive(): Boolean = isRunning.get()

    /**
     * Obtient les statistiques de synchronisation
     */
    fun getStats(): SyncStats {
        return SyncStats(
            attempts = syncAttempts,
            successes = syncSuccesses,
            failures = syncFailures,
            lastSyncTime = lastSyncTime,
            isRunning = isRunning.get()
        )
    }

    /**
     * Force une synchronisation immédiate (pour tests)
     */
    suspend fun forceSyncNow(): Boolean {
        return performTimeSync()
    }

    /**
     * Effectue une tentative de synchronisation du temps
     */
    private suspend fun performTimeSync(): Boolean = withContext(Dispatchers.IO) {
        syncAttempts++
        val startTime = System.currentTimeMillis()

        try {
            Log.d(TAG, "=== TENTATIVE SYNCHRONISATION #$syncAttempts ===")

            // 1. Vérifier si des périphériques USB sont connectés
            val devices = usbCdcManager.listConnectedDevices()
            if (devices.isEmpty()) {
                Log.d(TAG, "⚠️ Aucun périphérique USB connecté - synchronisation ignorée")
                syncFailures++
                return@withContext false
            }

            Log.d(TAG, "✅ ${devices.size} périphérique(s) USB détecté(s)")

            // 2. Générer le message de synchronisation du temps
            val timeSyncFrame = try {
                wristbandFrameManager.generateTimeSyncMessage()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la génération du message de temps: ${e.message}")
                syncFailures++
                return@withContext false
            }

            Log.d(TAG, "📅 Message de synchronisation généré: ${timeSyncFrame.size} octets")

            // 3. Envoyer le message via USB
            val success = try {
                usbCdcManager.sendBytes(timeSyncFrame)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de l'envoi USB: ${e.message}")
                syncFailures++
                return@withContext false
            }

            val duration = System.currentTimeMillis() - startTime

            if (success) {
                syncSuccesses++
                lastSyncTime = System.currentTimeMillis()
                Log.i(TAG, "✅ Synchronisation réussie en ${duration}ms (succès #$syncSuccesses)")
                return@withContext true
            } else {
                syncFailures++
                Log.w(TAG, "❌ Échec de synchronisation en ${duration}ms (échec #$syncFailures)")
                return@withContext false
            }

        } catch (e: Exception) {
            syncFailures++
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ Exception lors de la synchronisation en ${duration}ms: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Nettoie les ressources du service
     */
    fun cleanup() {
        stop()
        scope.cancel()
        Log.i(TAG, "🧹 Ressources du service de synchronisation nettoyées")
    }
}

/**
 * Classe de données pour les statistiques de synchronisation
 */
data class SyncStats(
    val attempts: Int,
    val successes: Int,
    val failures: Int,
    val lastSyncTime: Long,
    val isRunning: Boolean
) {
    val successRate: Int get() = if (attempts > 0) (successes * 100) / attempts else 0
    val failureRate: Int get() = if (attempts > 0) (failures * 100) / attempts else 0
}

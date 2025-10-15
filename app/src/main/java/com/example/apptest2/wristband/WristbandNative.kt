package com.example.apptest2.wristband

// Classes de données pour les couleurs et styles
data class WristbandColor(
    val red: Int,
    val green: Int,
    val blue: Int
)

enum class WristbandStyle {
    ON,
    STROBE,
    PULSE,
    HEARTBEAT
}

// Interface JNI pour la librairie C++ wristband_objects
class WristbandNative {

    companion object {
        // Charger la librairie native au démarrage
        init {
            System.loadLibrary("wristband_native")
        }
    }

    // Fonctions natives qui appellent la librairie C++ wristband_objects
    external fun createHelloMessage(sourceVersion: String, sourceName: String, destinationMask: Int): ByteArray
    external fun createEventMessage(style: Int, red: Int, green: Int, blue: Int): ByteArray
    external fun createCommandMessage(command: Int, param1: Int, param2: Int): ByteArray
    external fun validateFrame(frame: ByteArray): Boolean
    external fun getFrameInfo(frame: ByteArray): String

    // Fonction native étendue pour créer un Event complet avec tous les paramètres
    external fun createDetailedEventMessage(
        // Timing
        rStartEventMs: Long,
        rStopEventMs: Long,
        mask: Int,
        // Effect
        styleValue: Int,
        frequency: Int,
        duration: Int,
        intensity: Int,
        colorRed: Int,
        colorGreen: Int,
        colorBlue: Int,
        colorWhite: Int,
        colorVibration: Int,
        // Localization
        mapId: Int,
        focus: Int,
        zoom: Int,
        goboTypeValue: Int,
        // Layer
        layerNbr: Int,
        layerOpacity: Int,
        blendingModeValue: Int
    ): ByteArray

    // Fonction native pour la synchronisation automatique du temps
    external fun createTimeSyncMessage(): ByteArray

    // Fonction native pour envoyer la référence de temps relatif (temps d'ouverture de l'app)
    external fun createTimeReferenceMessage(referenceTimeMs: Long): ByteArray
}

// Gestionnaire des trames wristband
class WristbandFrameManager {
    private val wristbandNative = WristbandNative()

    fun initialize(): Boolean {
        return try {
            // Test simple pour vérifier que la librairie native fonctionne
            val testFrame = wristbandNative.createHelloMessage("1.0", "Android", 0)
            testFrame != null && testFrame.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.e("WristbandFrameManager", "Erreur d'initialisation: ${e.message}", e)
            false
        }
    }

    fun generateSimpleEvent(style: WristbandStyle, color: WristbandColor): ByteArray {
        try {
            val styleInt = when (style) {
                WristbandStyle.ON -> 0        // Style::On = 0
                WristbandStyle.STROBE -> 2    // Style::Strobe = 2
                WristbandStyle.PULSE -> 7     // Style::Pulse = 7
                WristbandStyle.HEARTBEAT -> 5 // Style::Heartbeat = 5
            }

            android.util.Log.d("WristbandFrameManager", "Génération Event: style=$styleInt, color=(${color.red},${color.green},${color.blue})")

            val result = wristbandNative.createEventMessage(styleInt, color.red, color.green, color.blue)

            if (result == null) {
                android.util.Log.e("WristbandFrameManager", "createEventMessage a retourné null")
                throw RuntimeException("Impossible de générer la trame Event")
            }

            android.util.Log.d("WristbandFrameManager", "Event généré avec succès: ${result.size} octets")
            return result

        } catch (e: Exception) {
            android.util.Log.e("WristbandFrameManager", "Erreur lors de la génération Event: ${e.message}", e)
            throw e
        }
    }

    fun generateHelloMessage(sourceVersion: String, sourceName: String, destinationMask: Int): ByteArray {
        return wristbandNative.createHelloMessage(sourceVersion, sourceName, destinationMask)
    }

    fun generateCommandMessage(command: Int, param1: Int, param2: Int): ByteArray {
        return wristbandNative.createCommandMessage(command, param1, param2)
    }

    fun validateFrame(frame: ByteArray): Boolean {
        return wristbandNative.validateFrame(frame)
    }

    fun frameToHexString(frame: ByteArray): String {
        return frame.joinToString(" ") { "0x%02x".format(it) }
    }

    fun getFrameInfo(frame: ByteArray): String {
        return wristbandNative.getFrameInfo(frame)
    }

    fun generateDetailedEvent(detailedEventConfig: com.example.apptest2.ui.DetailedEventConfig): ByteArray {
        try {
            android.util.Log.d("WristbandFrameManager", "Génération Event détaillé avec TOUS les paramètres")
            android.util.Log.d("WristbandFrameManager", "Config complète reçue:")
            android.util.Log.d("WristbandFrameManager", "  Timing: ${detailedEventConfig.rStartEventMs}-${detailedEventConfig.rStopEventMs}ms, mask=${detailedEventConfig.mask}")
            android.util.Log.d("WristbandFrameManager", "  Style: ${detailedEventConfig.effect.style} (${detailedEventConfig.effect.style.value})")
            android.util.Log.d("WristbandFrameManager", "  Fréquence: ${detailedEventConfig.effect.frequency}Hz, Durée: ${detailedEventConfig.effect.duration}ms")
            android.util.Log.d("WristbandFrameManager", "  Intensité: ${detailedEventConfig.effect.intensity}/255")
            android.util.Log.d("WristbandFrameManager", "  Couleur RGBWV: (${detailedEventConfig.effect.color.red},${detailedEventConfig.effect.color.green},${detailedEventConfig.effect.color.blue},${detailedEventConfig.effect.color.white},${detailedEventConfig.effect.color.vibration})")
            android.util.Log.d("WristbandFrameManager", "  Localisation: map=${detailedEventConfig.localization.mapId}, focus=${detailedEventConfig.localization.focus}, zoom=${detailedEventConfig.localization.zoom}, gobo=${detailedEventConfig.localization.goboType}")
            android.util.Log.d("WristbandFrameManager", "  Layer: nbr=${detailedEventConfig.layer.nbr}, opacity=${detailedEventConfig.layer.opacity}, blend=${detailedEventConfig.layer.blendingMode}")

            // Essayer d'abord d'utiliser createDetailedEventMessage avec TOUS les paramètres
            try {
                android.util.Log.d("WristbandFrameManager", "🚀 Tentative d'utilisation de createDetailedEventMessage avec tous les paramètres")

                val result = wristbandNative.createDetailedEventMessage(
                    // Timing
                    detailedEventConfig.rStartEventMs,
                    detailedEventConfig.rStopEventMs,
                    detailedEventConfig.mask,
                    // Effect
                    detailedEventConfig.effect.style.value,
                    detailedEventConfig.effect.frequency,
                    detailedEventConfig.effect.duration,
                    detailedEventConfig.effect.intensity,
                    detailedEventConfig.effect.color.red,
                    detailedEventConfig.effect.color.green,
                    detailedEventConfig.effect.color.blue,
                    detailedEventConfig.effect.color.white,
                    detailedEventConfig.effect.color.vibration,
                    // Localization
                    detailedEventConfig.localization.mapId,
                    detailedEventConfig.localization.focus,
                    detailedEventConfig.localization.zoom,
                    detailedEventConfig.localization.goboType.value,
                    // Layer
                    detailedEventConfig.layer.nbr,
                    detailedEventConfig.layer.opacity,
                    detailedEventConfig.layer.blendingMode.value
                )

                if (result == null) {
                    android.util.Log.e("WristbandFrameManager", "createDetailedEventMessage a retourné null")
                    throw RuntimeException("Impossible de générer la trame Event détaillée")
                }

                android.util.Log.d("WristbandFrameManager", "✅ Event détaillé généré avec succès avec TOUS les paramètres: ${result.size} octets")
                android.util.Log.d("WristbandFrameManager", "✅ TOUS les paramètres ont été transmis:")
                android.util.Log.d("WristbandFrameManager", "  ✅ Timing: ${detailedEventConfig.rStartEventMs}-${detailedEventConfig.rStopEventMs}ms, mask=${detailedEventConfig.mask}")
                android.util.Log.d("WristbandFrameManager", "  ✅ Effet: style=${detailedEventConfig.effect.style.value}, freq=${detailedEventConfig.effect.frequency}Hz, dur=${detailedEventConfig.effect.duration}ms, int=${detailedEventConfig.effect.intensity}")
                android.util.Log.d("WristbandFrameManager", "  ✅ Couleur complète: RGBWV(${detailedEventConfig.effect.color.red},${detailedEventConfig.effect.color.green},${detailedEventConfig.effect.color.blue},${detailedEventConfig.effect.color.white},${detailedEventConfig.effect.color.vibration})")
                android.util.Log.d("WristbandFrameManager", "  ✅ Localisation: map=${detailedEventConfig.localization.mapId}, focus=${detailedEventConfig.localization.focus}, zoom=${detailedEventConfig.localization.zoom}, gobo=${detailedEventConfig.localization.goboType.value}")
                android.util.Log.d("WristbandFrameManager", "  ✅ Layer: nbr=${detailedEventConfig.layer.nbr}, opacity=${detailedEventConfig.layer.opacity}, blend=${detailedEventConfig.layer.blendingMode.value}")
                android.util.Log.d("WristbandFrameManager", "Trame détaillée: ${frameToHexString(result)}")

                return result

            } catch (e: UnsatisfiedLinkError) {
                // La fonction createDetailedEventMessage n'est pas encore implémentée côté C++
                android.util.Log.w("WristbandFrameManager", "⚠️ createDetailedEventMessage pas encore implémentée côté C++")
                android.util.Log.w("WristbandFrameManager", "🔄 Fallback vers createEventMessage avec paramètres de base")

                // Fallback : utiliser la fonction simple avec les paramètres de base
                val styleInt = detailedEventConfig.effect.style.value
                val red = detailedEventConfig.effect.color.red
                val green = detailedEventConfig.effect.color.green
                val blue = detailedEventConfig.effect.color.blue

                android.util.Log.d("WristbandFrameManager", "Paramètres de base transmis: style=$styleInt, RGB($red,$green,$blue)")

                val result = wristbandNative.createEventMessage(styleInt, red, green, blue)

                if (result == null) {
                    android.util.Log.e("WristbandFrameManager", "createEventMessage a retourné null")
                    throw RuntimeException("Impossible de générer la trame Event")
                }

                android.util.Log.d("WristbandFrameManager", "Event généré avec succès (mode fallback): ${result.size} octets")
                android.util.Log.d("WristbandFrameManager", "Trame: ${frameToHexString(result)}")

                // Log des paramètres non utilisés en mode fallback
                android.util.Log.i("WristbandFrameManager", "📋 Paramètres configurés mais non transmis (en attente d'implémentation C++):")
                if (detailedEventConfig.rStartEventMs != 0L || detailedEventConfig.rStopEventMs != 1000L) {
                    android.util.Log.i("WristbandFrameManager", "  📋 Timing: ${detailedEventConfig.rStartEventMs}-${detailedEventConfig.rStopEventMs}ms")
                }
                if (detailedEventConfig.mask != 0) {
                    android.util.Log.i("WristbandFrameManager", "  📋 Masque: ${detailedEventConfig.mask}")
                }
                if (detailedEventConfig.effect.frequency != 1) {
                    android.util.Log.i("WristbandFrameManager", "  📋 Fréquence: ${detailedEventConfig.effect.frequency}Hz")
                }
                if (detailedEventConfig.effect.duration != 100) {
                    android.util.Log.i("WristbandFrameManager", "  📋 Durée: ${detailedEventConfig.effect.duration}ms")
                }
                if (detailedEventConfig.effect.intensity != 255) {
                    android.util.Log.i("WristbandFrameManager", "  📋 Intensité: ${detailedEventConfig.effect.intensity}")
                }
                if (detailedEventConfig.effect.color.white != 0 || detailedEventConfig.effect.color.vibration != 0) {
                    android.util.Log.i("WristbandFrameManager", "  📋 Blanc/Vibration: ${detailedEventConfig.effect.color.white}/${detailedEventConfig.effect.color.vibration}")
                }
                if (detailedEventConfig.localization.mapId != 0 || detailedEventConfig.localization.focus != 0 || detailedEventConfig.localization.zoom != 10) {
                    android.util.Log.i("WristbandFrameManager", "  📋 Localisation: map=${detailedEventConfig.localization.mapId}, focus=${detailedEventConfig.localization.focus}, zoom=${detailedEventConfig.localization.zoom}")
                }
                if (detailedEventConfig.layer.nbr != 0 || detailedEventConfig.layer.opacity != 255) {
                    android.util.Log.i("WristbandFrameManager", "  📋 Layer: nbr=${detailedEventConfig.layer.nbr}, opacity=${detailedEventConfig.layer.opacity}")
                }

                return result
            }

        } catch (e: Exception) {
            android.util.Log.e("WristbandFrameManager", "Erreur lors de la génération Event détaillé: ${e.message}", e)
            throw e
        }
    }

    // Nouvelle méthode pour générer un message de synchronisation du temps
    fun generateTimeSyncMessage(): ByteArray {
        try {
            android.util.Log.d("WristbandFrameManager", "Génération message de synchronisation du temps")

            val result = wristbandNative.createTimeSyncMessage()

            if (result == null) {
                android.util.Log.e("WristbandFrameManager", "createTimeSyncMessage a retourné null")
                throw RuntimeException("Impossible de générer le message de synchronisation du temps")
            }

            android.util.Log.d("WristbandFrameManager", "Message de synchronisation du temps généré avec succès: ${result.size} octets")
            android.util.Log.d("WristbandFrameManager", "Trame temps: ${frameToHexString(result)}")

            return result

        } catch (e: Exception) {
            android.util.Log.e("WristbandFrameManager", "Erreur lors de la génération du message de synchronisation: ${e.message}", e)
            throw e
        }
    }

    // Nouvelle méthode pour envoyer la référence de temps relatif
    fun generateTimeReferenceMessage(referenceTimeMs: Long): ByteArray {
        try {
            android.util.Log.d("WristbandFrameManager", "Envoi de la référence de temps relatif: ${referenceTimeMs}ms")

            val result = wristbandNative.createTimeReferenceMessage(referenceTimeMs)

            if (result == null) {
                android.util.Log.e("WristbandFrameManager", "createTimeReferenceMessage a retourné null")
                throw RuntimeException("Impossible d'envoyer la référence de temps")
            }

            android.util.Log.d("WristbandFrameManager", "Référence de temps envoyée avec succès: ${result.size} octets")
            android.util.Log.d("WristbandFrameManager", "Trame référence temps: ${frameToHexString(result)}")

            return result

        } catch (e: Exception) {
            android.util.Log.e("WristbandFrameManager", "Erreur lors de l'envoi de la référence de temps: ${e.message}", e)
            throw e
        }
    }

    // Nouvelle méthode pour générer un événement avec temps relatifs calculés automatiquement
    fun generateDetailedEventWithRelativeTime(
        detailedEventConfig: com.example.apptest2.ui.DetailedEventConfig,
        applicationStartTimeMs: Long
    ): ByteArray {
        try {
            // Calculer le temps actuel relatif à l'ouverture de l'application
            val currentTimeMs = System.currentTimeMillis()
            val relativeCurrentTimeMs = currentTimeMs - applicationStartTimeMs

            // Calculer les temps relatifs pour l'événement
            val relativeStartEventMs = relativeCurrentTimeMs + detailedEventConfig.rStartEventMs
            val relativeStopEventMs = relativeCurrentTimeMs + detailedEventConfig.rStopEventMs

            android.util.Log.d("WristbandFrameManager", "=== CALCUL TEMPS RELATIFS ===")
            android.util.Log.d("WristbandFrameManager", "Temps référence application: ${applicationStartTimeMs}ms")
            android.util.Log.d("WristbandFrameManager", "Temps actuel: ${currentTimeMs}ms")
            android.util.Log.d("WristbandFrameManager", "Temps relatif actuel: ${relativeCurrentTimeMs}ms")
            android.util.Log.d("WristbandFrameManager", "Temps origine config: start=${detailedEventConfig.rStartEventMs}ms, stop=${detailedEventConfig.rStopEventMs}ms")
            android.util.Log.d("WristbandFrameManager", "Temps relatifs calculés: start=${relativeStartEventMs}ms, stop=${relativeStopEventMs}ms")

            // Créer une nouvelle configuration avec les temps relatifs calculés
            val relativeConfig = detailedEventConfig.copy(
                rStartEventMs = relativeStartEventMs,
                rStopEventMs = relativeStopEventMs
            )

            android.util.Log.d("WristbandFrameManager", "Génération Event détaillé avec temps relatifs")
            android.util.Log.d("WristbandFrameManager", "Config avec temps relatifs:")
            android.util.Log.d("WristbandFrameManager", "  Timing relatif: ${relativeConfig.rStartEventMs}-${relativeConfig.rStopEventMs}ms, mask=${relativeConfig.mask}")
            android.util.Log.d("WristbandFrameManager", "  Style: ${relativeConfig.effect.style} (${relativeConfig.effect.style.value})")
            android.util.Log.d("WristbandFrameManager", "  Fréquence: ${relativeConfig.effect.frequency}Hz, Durée: ${relativeConfig.effect.duration}ms")
            android.util.Log.d("WristbandFrameManager", "  Intensité: ${relativeConfig.effect.intensity}/255")
            android.util.Log.d("WristbandFrameManager", "  Couleur RGBWV: (${relativeConfig.effect.color.red},${relativeConfig.effect.color.green},${relativeConfig.effect.color.blue},${relativeConfig.effect.color.white},${relativeConfig.effect.color.vibration})")

            // Essayer d'abord d'utiliser createDetailedEventMessage avec TOUS les paramètres
            try {
                android.util.Log.d("WristbandFrameManager", "🚀 Tentative d'utilisation de createDetailedEventMessage avec temps relatifs")

                val result = wristbandNative.createDetailedEventMessage(
                    // Timing - AVEC TEMPS RELATIFS
                    relativeConfig.rStartEventMs,
                    relativeConfig.rStopEventMs,
                    relativeConfig.mask,
                    // Effect
                    relativeConfig.effect.style.value,
                    relativeConfig.effect.frequency,
                    relativeConfig.effect.duration,
                    relativeConfig.effect.intensity,
                    relativeConfig.effect.color.red,
                    relativeConfig.effect.color.green,
                    relativeConfig.effect.color.blue,
                    relativeConfig.effect.color.white,
                    relativeConfig.effect.color.vibration,
                    // Localization
                    relativeConfig.localization.mapId,
                    relativeConfig.localization.focus,
                    relativeConfig.localization.zoom,
                    relativeConfig.localization.goboType.value,
                    // Layer
                    relativeConfig.layer.nbr,
                    relativeConfig.layer.opacity,
                    relativeConfig.layer.blendingMode.value
                )

                if (result == null) {
                    android.util.Log.e("WristbandFrameManager", "createDetailedEventMessage a retourné null")
                    throw RuntimeException("Impossible de générer la trame Event détaillée avec temps relatifs")
                }

                android.util.Log.d("WristbandFrameManager", "✅ Event détaillé avec temps relatifs généré avec succès: ${result.size} octets")
                android.util.Log.d("WristbandFrameManager", "✅ TEMPS RELATIFS transmis:")
                android.util.Log.d("WristbandFrameManager", "  ✅ Timing relatif: ${relativeConfig.rStartEventMs}-${relativeConfig.rStopEventMs}ms (calculé depuis référence app)")
                android.util.Log.d("WristbandFrameManager", "  ✅ Effet: style=${relativeConfig.effect.style.value}, freq=${relativeConfig.effect.frequency}Hz, dur=${relativeConfig.effect.duration}ms, int=${relativeConfig.effect.intensity}")
                android.util.Log.d("WristbandFrameManager", "  ✅ Couleur complète: RGBWV(${relativeConfig.effect.color.red},${relativeConfig.effect.color.green},${relativeConfig.effect.color.blue},${relativeConfig.effect.color.white},${relativeConfig.effect.color.vibration})")
                android.util.Log.d("WristbandFrameManager", "Trame avec temps relatifs: ${frameToHexString(result)}")

                return result

            } catch (e: UnsatisfiedLinkError) {
                // La fonction createDetailedEventMessage n'est pas encore implémentée côté C++
                android.util.Log.w("WristbandFrameManager", "⚠️ createDetailedEventMessage pas encore implémentée côté C++")
                android.util.Log.w("WristbandFrameManager", "🔄 Fallback vers createEventMessage avec paramètres de base (temps relatifs ignorés)")

                // Fallback : utiliser la fonction simple avec les paramètres de base
                val styleInt = relativeConfig.effect.style.value
                val red = relativeConfig.effect.color.red
                val green = relativeConfig.effect.color.green
                val blue = relativeConfig.effect.color.blue

                android.util.Log.d("WristbandFrameManager", "Paramètres de base transmis: style=$styleInt, RGB($red,$green,$blue)")
                android.util.Log.w("WristbandFrameManager", "⚠️ Temps relatifs ${relativeConfig.rStartEventMs}-${relativeConfig.rStopEventMs}ms non transmis (fonction C++ manquante)")

                val result = wristbandNative.createEventMessage(styleInt, red, green, blue)

                if (result == null) {
                    android.util.Log.e("WristbandFrameManager", "createEventMessage a retourné null")
                    throw RuntimeException("Impossible de générer la trame Event")
                }

                android.util.Log.d("WristbandFrameManager", "Event généré avec succès (mode fallback): ${result.size} octets")
                android.util.Log.d("WristbandFrameManager", "Trame: ${frameToHexString(result)}")

                return result
            }

        } catch (e: Exception) {
            android.util.Log.e("WristbandFrameManager", "Erreur lors de la génération Event avec temps relatifs: ${e.message}", e)
            throw e
        }
    }
}

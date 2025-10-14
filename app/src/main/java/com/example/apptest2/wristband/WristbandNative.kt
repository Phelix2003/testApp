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
}

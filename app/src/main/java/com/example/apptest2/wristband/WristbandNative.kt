package com.example.apptest2.wristband

/**
 * Interface native pour la librairie wristband_objects
 * Cette classe permet de générer et manipuler les trames du protocole wristband
 */
class WristbandNative {

    companion object {
        // Charger la librairie native au démarrage
        init {
            System.loadLibrary("wristband_native")
        }
    }

    /**
     * Initialise la librairie wristband_objects
     * @return true si l'initialisation s'est bien passée
     */
    external fun initialize(): Boolean

    /**
     * Génère une trame selon le protocole wristband
     * @param frameType Type de trame à générer
     * @param data Données à inclure dans la trame
     * @return La trame générée prête à être envoyée
     */
    external fun generateFrame(frameType: Int, data: String): String

    /**
     * Valide une trame selon le protocole wristband
     * @param frame Trame à valider
     * @return true si la trame est valide
     */
    external fun validateFrame(frame: String): Boolean

    /**
     * Parse une trame reçue selon le protocole wristband
     * @param frame Trame à parser
     * @return Les données parsées ou une chaîne vide en cas d'erreur
     */
    external fun parseFrame(frame: String): String
}

/**
 * Gestionnaire de haut niveau pour les trames wristband
 * Encapsule l'interface native et fournit des méthodes pratiques
 */
class WristbandFrameManager {
    private val nativeInterface = WristbandNative()
    private var isInitialized = false

    /**
     * Types de trames disponibles
     */
    object FrameTypes {
        const val COMMAND = 1
        const val DATA = 2
        const val STATUS = 3
        const val ACK = 4
        const val ERROR = 5
    }

    /**
     * Initialise le gestionnaire de trames
     */
    fun initialize(): Boolean {
        if (!isInitialized) {
            isInitialized = nativeInterface.initialize()
        }
        return isInitialized
    }

    /**
     * Génère une trame de commande
     */
    fun generateCommandFrame(command: String): String {
        ensureInitialized()
        return nativeInterface.generateFrame(FrameTypes.COMMAND, command)
    }

    /**
     * Génère une trame de données
     */
    fun generateDataFrame(data: String): String {
        ensureInitialized()
        return nativeInterface.generateFrame(FrameTypes.DATA, data)
    }

    /**
     * Génère une trame de statut
     */
    fun generateStatusFrame(status: String): String {
        ensureInitialized()
        return nativeInterface.generateFrame(FrameTypes.STATUS, status)
    }

    /**
     * Génère une trame d'acquittement
     */
    fun generateAckFrame(ackData: String = ""): String {
        ensureInitialized()
        return nativeInterface.generateFrame(FrameTypes.ACK, ackData)
    }

    /**
     * Génère une trame d'erreur
     */
    fun generateErrorFrame(errorMsg: String): String {
        ensureInitialized()
        return nativeInterface.generateFrame(FrameTypes.ERROR, errorMsg)
    }

    /**
     * Valide une trame reçue
     */
    fun validateFrame(frame: String): Boolean {
        ensureInitialized()
        return nativeInterface.validateFrame(frame)
    }

    /**
     * Parse une trame reçue et retourne les données
     */
    fun parseFrame(frame: String): String {
        ensureInitialized()
        return nativeInterface.parseFrame(frame)
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("WristbandFrameManager n'est pas initialisé. Appelez initialize() d'abord.")
        }
    }
}

package com.example.apptest2.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.apptest2.ui.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Gestionnaire de sauvegarde et chargement des configurations des boutons
 * Utilise SharedPreferences avec sérialisation JSON pour persister les données
 */
class ConfigurationStorage(context: Context) {

    companion object {
        private const val TAG = "ConfigurationStorage"
        private const val PREFS_NAME = "wristband_button_configs"
        private const val KEY_CONFIGS = "button_configurations"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Sauvegarde toutes les configurations des boutons
     */
    fun saveConfigurations(configs: Map<Int, MessageConfig>) {
        try {
            Log.d(TAG, "💾 Sauvegarde de ${configs.size} configurations de boutons")

            // Convertir en format sérialisable
            val serializableConfigs = configs.mapValues { (buttonNumber, config) ->
                SerializableMessageConfig.fromMessageConfig(config, buttonNumber)
            }

            val jsonString = json.encodeToString(serializableConfigs)

            preferences.edit()
                .putString(KEY_CONFIGS, jsonString)
                .apply()

            Log.i(TAG, "✅ Configurations sauvegardées avec succès")
            Log.d(TAG, "Taille JSON: ${jsonString.length} caractères")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la sauvegarde des configurations: ${e.message}", e)
        }
    }

    /**
     * Charge toutes les configurations des boutons
     * Retourne les configurations par défaut si aucune sauvegarde n'existe
     */
    fun loadConfigurations(): MutableMap<Int, MessageConfig> {
        try {
            val jsonString = preferences.getString(KEY_CONFIGS, null)

            if (jsonString == null) {
                Log.i(TAG, "📂 Aucune configuration sauvegardée trouvée - utilisation des valeurs par défaut")
                return DefaultMessageConfigs.configs.toMutableMap()
            }

            Log.d(TAG, "📖 Chargement des configurations sauvegardées")
            Log.d(TAG, "Taille JSON: ${jsonString.length} caractères")

            val serializableConfigs: Map<String, SerializableMessageConfig> = json.decodeFromString(jsonString)

            val configs = mutableMapOf<Int, MessageConfig>()

            serializableConfigs.forEach { (buttonNumberStr, serializableConfig) ->
                try {
                    val buttonNumber = buttonNumberStr.toInt()
                    val messageConfig = serializableConfig.toMessageConfig()
                    configs[buttonNumber] = messageConfig

                    Log.d(TAG, "✅ Configuration bouton $buttonNumber chargée: ${messageConfig.name}")

                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erreur lors du chargement de la configuration du bouton $buttonNumberStr: ${e.message}")
                }
            }

            // Compléter avec les configurations par défaut si des boutons manquent
            for (buttonNumber in 1..8) {
                if (!configs.containsKey(buttonNumber)) {
                    val defaultConfig = DefaultMessageConfigs.configs[buttonNumber]
                    if (defaultConfig != null) {
                        configs[buttonNumber] = defaultConfig
                        Log.d(TAG, "➕ Configuration par défaut ajoutée pour le bouton $buttonNumber")
                    }
                }
            }

            Log.i(TAG, "✅ ${configs.size} configurations chargées avec succès")
            return configs

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du chargement des configurations: ${e.message}", e)
            Log.i(TAG, "🔄 Utilisation des configurations par défaut en fallback")
            return DefaultMessageConfigs.configs.toMutableMap()
        }
    }

    /**
     * Efface toutes les configurations sauvegardées
     */
    fun clearConfigurations() {
        try {
            preferences.edit().remove(KEY_CONFIGS).apply()
            Log.i(TAG, "🗑️ Toutes les configurations ont été effacées")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'effacement des configurations: ${e.message}", e)
        }
    }

    /**
     * Vérifie si des configurations sauvegardées existent
     */
    fun hasStoredConfigurations(): Boolean {
        return preferences.contains(KEY_CONFIGS)
    }
}

/**
 * Classes sérialisables pour la sauvegarde JSON
 * Nécessaires car les enums et data classes de l'interface ne sont pas sérialisables par défaut
 */
@Serializable
data class SerializableMessageConfig(
    val name: String,
    val rStartEventMs: Long,
    val rStopEventMs: Long,
    val mask: Int,
    // Effect
    val styleValue: Int,
    val frequency: Int,
    val duration: Int,
    val intensity: Int,
    // Color
    val colorRed: Int,
    val colorGreen: Int,
    val colorBlue: Int,
    val colorWhite: Int,
    val colorVibration: Int,
    // Localization
    val mapId: Int,
    val focus: Int,
    val zoom: Int,
    val goboTypeValue: Int,
    // Layer
    val layerNbr: Int,
    val layerOpacity: Int,
    val blendingModeValue: Int
) {
    companion object {
        fun fromMessageConfig(config: MessageConfig, buttonNumber: Int): SerializableMessageConfig {
            return SerializableMessageConfig(
                name = config.name,
                rStartEventMs = config.detailedEventConfig.rStartEventMs,
                rStopEventMs = config.detailedEventConfig.rStopEventMs,
                mask = config.detailedEventConfig.mask,
                // Effect
                styleValue = config.detailedEventConfig.effect.style.value,
                frequency = config.detailedEventConfig.effect.frequency,
                duration = config.detailedEventConfig.effect.duration,
                intensity = config.detailedEventConfig.effect.intensity,
                // Color
                colorRed = config.detailedEventConfig.effect.color.red,
                colorGreen = config.detailedEventConfig.effect.color.green,
                colorBlue = config.detailedEventConfig.effect.color.blue,
                colorWhite = config.detailedEventConfig.effect.color.white,
                colorVibration = config.detailedEventConfig.effect.color.vibration,
                // Localization
                mapId = config.detailedEventConfig.localization.mapId,
                focus = config.detailedEventConfig.localization.focus,
                zoom = config.detailedEventConfig.localization.zoom,
                goboTypeValue = config.detailedEventConfig.localization.goboType.value,
                // Layer
                layerNbr = config.detailedEventConfig.layer.nbr,
                layerOpacity = config.detailedEventConfig.layer.opacity,
                blendingModeValue = config.detailedEventConfig.layer.blendingMode.value
            )
        }
    }

    fun toMessageConfig(): MessageConfig {
        return MessageConfig(
            name = name,
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = rStartEventMs,
                rStopEventMs = rStopEventMs,
                mask = mask,
                effect = DetailedEffectConfig(
                    style = EventStyle.values().find { it.value == styleValue } ?: EventStyle.ON,
                    frequency = frequency,
                    duration = duration,
                    intensity = intensity,
                    color = EventColor(
                        red = colorRed,
                        green = colorGreen,
                        blue = colorBlue,
                        white = colorWhite,
                        vibration = colorVibration
                    )
                ),
                localization = LocalizationConfig(
                    mapId = mapId,
                    focus = focus,
                    zoom = zoom,
                    goboType = GoboType.values().find { it.value == goboTypeValue } ?: GoboType.NONE
                ),
                layer = LayerConfig(
                    nbr = layerNbr,
                    opacity = layerOpacity,
                    blendingMode = BlendingMode.values().find { it.value == blendingModeValue } ?: BlendingMode.NORMAL
                )
            )
        )
    }
}

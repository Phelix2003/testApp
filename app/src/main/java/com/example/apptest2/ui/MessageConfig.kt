package com.example.apptest2.ui

// Énumérations correspondant aux enum C++ du fichier Event.h
enum class EventStyle(val value: Int) {
    ON(0),
    OFF(1),
    STROBE(2),
    WAVE(3),
    PW(4),
    HEARTBEAT(5),
    SPARKLE(6),
    PULSE(7),
    RANDOM(8),
    ACC_ON(9),
    ACC_ON_XYZ(10),
    ACC_CLAP(11),
    ACC_POSITION(12),
    ACC_FLASH(13),
    CLEAR(14),
    ACC_HOLO(15),
    BOOM(16)
}

enum class BlendingMode(val value: Int) {
    NORMAL(0),
    ADD(1),
    AND(2),
    SUBSTRACT(3),
    MULTIPLY(4),
    DIVIDE(5)
}

enum class GoboType(val value: Int) {
    NONE(0),
    POINT(1),
    LINE(2),
    POLYGON(3)
}

// Configuration pour les couleurs RGBW + Vibration (5 dimensions)
data class EventColor(
    val red: Int = 255,
    val green: Int = 0,
    val blue: Int = 0,
    val white: Int = 0,
    val vibration: Int = 0
) {
    init {
        require(red in 0..255) { "Rouge doit être entre 0 et 255" }
        require(green in 0..255) { "Vert doit être entre 0 et 255" }
        require(blue in 0..255) { "Bleu doit être entre 0 et 255" }
        require(white in 0..255) { "Blanc doit être entre 0 et 255" }
        require(vibration in 0..255) { "Vibration doit être entre 0 et 255" }
    }
}

// Sous-groupe Effect
data class DetailedEffectConfig(
    val style: EventStyle = EventStyle.ON,
    val frequency: Int = 1,      // 1Hz à 20Hz
    val duration: Int = 100,     // en ms
    val intensity: Int = 255,    // 0-255
    val color: EventColor = EventColor()
) {
    init {
        require(frequency in 1..255) { "Fréquence doit être entre 1 et 255" }
        require(duration in 0..255) { "Durée doit être entre 0 et 255 ms" }
        require(intensity in 0..255) { "Intensité doit être entre 0 et 255" }
    }
}

// Sous-groupe Layer
data class LayerConfig(
    val nbr: Int = 0,            // Numéro de layer
    val opacity: Int = 255,      // 0 transparent - 255 opaque
    val blendingMode: BlendingMode = BlendingMode.NORMAL
) {
    init {
        require(nbr in 0..255) { "Numéro de layer doit être entre 0 et 255" }
        require(opacity in 0..255) { "Opacité doit être entre 0 et 255" }
    }
}

// Sous-groupe Localization
data class LocalizationConfig(
    val mapId: Int = 0,          // ID de la carte GPS
    val focus: Int = 0,          // Facteur de fade en pas de 10cm (0-255)
    val zoom: Int = 10,          // Profondeur d'effet en pas de 10cm (0-255)
    val goboType: GoboType = GoboType.NONE
) {
    init {
        require(mapId in 0..255) { "Map ID doit être entre 0 et 255" }
        require(focus in 0..255) { "Focus doit être entre 0 et 255 (0-25.5m)" }
        require(zoom in 0..255) { "Zoom doit être entre 0 et 255 (0-25.5m)" }
    }
}

// Configuration Event détaillée
data class DetailedEventConfig(
    val rStartEventMs: Long = 0,     // Temps de début en ms
    val rStopEventMs: Long = 1000,   // Temps de fin en ms
    val mask: Int = 0,               // Masque d'adresse
    val localization: LocalizationConfig = LocalizationConfig(),
    val effect: DetailedEffectConfig = DetailedEffectConfig(),
    val layer: LayerConfig = LayerConfig()
) {
    init {
        require(rStartEventMs >= 0) { "Temps de début doit être positif" }
        require(rStopEventMs > rStartEventMs) { "Temps de fin doit être supérieur au temps de début" }
        require(mask in 0..255) { "Masque doit être entre 0 et 255" }
    }
}

// Configuration complète d'un message (uniquement événements détaillés)
data class MessageConfig(
    val name: String = "Message",
    val detailedEventConfig: DetailedEventConfig = DetailedEventConfig()
)

// Configuration par défaut pour les 8 boutons
object DefaultMessageConfigs {
    val configs = mutableMapOf(
        1 to MessageConfig(
            name = "Event 1",
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = 0,
                rStopEventMs = 5000,
                mask = 1,
                effect = DetailedEffectConfig(
                    style = EventStyle.ON,
                    frequency = 1,
                    duration = 100,
                    intensity = 255,
                    color = EventColor(255, 0, 0, 0, 0)
                ),
                localization = LocalizationConfig(
                    mapId = 0,
                    focus = 10,
                    zoom = 20,
                    goboType = GoboType.NONE
                ),
                layer = LayerConfig(
                    nbr = 0,
                    opacity = 255,
                    blendingMode = BlendingMode.NORMAL
                )
            )
        ),
        2 to MessageConfig(
            name = "Event 2",
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = 0,
                rStopEventMs = 3000,
                mask = 2,
                effect = DetailedEffectConfig(
                    style = EventStyle.STROBE,
                    frequency = 5,
                    duration = 50,
                    intensity = 200,
                    color = EventColor(0, 255, 0, 0, 0)
                )
            )
        ),
        3 to MessageConfig(
            name = "Event 3",
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = 0,
                rStopEventMs = 2000,
                mask = 4,
                effect = DetailedEffectConfig(
                    style = EventStyle.PULSE,
                    frequency = 2,
                    duration = 200,
                    intensity = 180,
                    color = EventColor(0, 0, 255, 0, 0)
                )
            )
        ),
        4 to MessageConfig(
            name = "Event 4",
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = 0,
                rStopEventMs = 1000,
                mask = 8,
                effect = DetailedEffectConfig(
                    style = EventStyle.HEARTBEAT,
                    frequency = 3,
                    duration = 150,
                    intensity = 255,
                    color = EventColor(255, 255, 255, 100, 50)
                )
            )
        ),
        5 to MessageConfig(
            name = "Event 5",
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = 0,
                rStopEventMs = 4000,
                mask = 16,
                effect = DetailedEffectConfig(
                    style = EventStyle.WAVE,
                    frequency = 4,
                    duration = 75,
                    intensity = 220,
                    color = EventColor(255, 100, 0, 0, 30)
                )
            )
        ),
        6 to MessageConfig(
            name = "Event 6",
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = 0,
                rStopEventMs = 6000,
                mask = 32,
                effect = DetailedEffectConfig(
                    style = EventStyle.SPARKLE,
                    frequency = 6,
                    duration = 120,
                    intensity = 200,
                    color = EventColor(100, 0, 255, 50, 0)
                )
            )
        ),
        7 to MessageConfig(
            name = "Event 7",
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = 0,
                rStopEventMs = 3500,
                mask = 64,
                effect = DetailedEffectConfig(
                    style = EventStyle.RANDOM,
                    frequency = 8,
                    duration = 90,
                    intensity = 190,
                    color = EventColor(255, 255, 0, 0, 20)
                )
            )
        ),
        8 to MessageConfig(
            name = "Event 8",
            detailedEventConfig = DetailedEventConfig(
                rStartEventMs = 0,
                rStopEventMs = 2500,
                mask = 128,
                effect = DetailedEffectConfig(
                    style = EventStyle.BOOM,
                    frequency = 10,
                    duration = 200,
                    intensity = 255,
                    color = EventColor(255, 0, 255, 80, 100)
                )
            )
        )
    )
}

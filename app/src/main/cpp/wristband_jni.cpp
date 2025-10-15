#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "wristband_obje/control.h"
#include "wristband_obje/Event.h"

#define LOG_TAG "WristbandJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Encapsule les données avec le protocole attendu par le device :
 * - Header: $$ (2 octets)
 * - Données: payload (taille variable)
 * - Parity: XOR de tous les octets de données (1 octet)
 * - Footer: 0xFF 0xAB 0xCD 0xEF (4 octets)
 */
std::vector<uint8_t> encapsulateMessage(const std::vector<uint8_t>& payload) {
    std::vector<uint8_t> encapsulated;

    // Réserver la taille nécessaire pour éviter les réallocations
    encapsulated.reserve(payload.size() + 7); // 2 (header) + 1 (parity) + 4 (footer)

    // 1. Ajouter le header '$$'
    encapsulated.push_back('$');
    encapsulated.push_back('$');

    // 2. Ajouter les données
    encapsulated.insert(encapsulated.end(), payload.begin(), payload.end());

    // 3. Calculer et ajouter la parité (XOR de tous les octets de données)
    uint8_t parity = 0;
    for (uint8_t byte : payload) {
        parity ^= byte;
    }
    encapsulated.push_back(parity);

    // 4. Ajouter le footer 0xFF 0xAB 0xCD 0xEF
    encapsulated.push_back(0xFF);
    encapsulated.push_back(0xAB);
    encapsulated.push_back(0xCD);
    encapsulated.push_back(0xEF);

    LOGI("Message encapsulé: payload=%zu octets, total=%zu octets, parity=0x%02X",
         payload.size(), encapsulated.size(), parity);

    return encapsulated;
}

extern "C" {

JNIEXPORT jbyteArray JNICALL
Java_com_example_apptest2_wristband_WristbandNative_createHelloMessage(
        JNIEnv *env,
        jobject /* this */,
        jstring sourceVersion,
        jstring sourceName,
        jint destinationMask) {

    try {
        LOGI("Début création Hello message");

        const char *version = env->GetStringUTFChars(sourceVersion, nullptr);
        const char *name = env->GetStringUTFChars(sourceName, nullptr);

        LOGI("Version: %s, Name: %s, Mask: %d", version, name, destinationMask);

        // Créer un message Hello avec la librairie wristband_objects
        Hello hello(version, name, static_cast<uint16_t>(destinationMask));
        std::vector<uint8_t> payload = hello.encode();

        LOGI("Hello payload créé, taille: %zu octets", payload.size());

        // Libérer les chaînes JNI
        env->ReleaseStringUTFChars(sourceVersion, version);
        env->ReleaseStringUTFChars(sourceName, name);

        // Encapsuler le message avec le protocole
        std::vector<uint8_t> frame = encapsulateMessage(payload);

        LOGI("Hello message encapsulé, taille totale: %zu octets", frame.size());

        // Convertir le std::vector en jbyteArray
        jbyteArray result = env->NewByteArray(frame.size());
        env->SetByteArrayRegion(result, 0, frame.size(), reinterpret_cast<const jbyte*>(frame.data()));

        return result;
    } catch (const std::exception& e) {
        LOGE("Exception dans createHelloMessage: %s", e.what());
        return nullptr;
    } catch (...) {
        LOGE("Exception inconnue dans createHelloMessage");
        return nullptr;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_example_apptest2_wristband_WristbandNative_createEventMessage(
        JNIEnv *env,
        jobject /* this */,
        jint style,
        jint red,
        jint green,
        jint blue) {

    try {
        LOGI("Début création Event message - Style: %d, RGB: %d,%d,%d", style, red, green, blue);

        // Créer un événement simple avec la librairie wristband_objects
        Event event;

        // Créer un effet avec le style demandé
        Style eventStyle = static_cast<Style>(style);
        uint8_t color[NB_COLORS_V0_0] = {
            static_cast<uint8_t>(red),   // Rouge
            static_cast<uint8_t>(green), // Vert
            static_cast<uint8_t>(blue),  // Bleu
            0,                           // Blanc
            0                            // Vibration
        };

        LOGI("Création Effect avec style %d", static_cast<int>(eventStyle));
        Effect effect(eventStyle, 1, 100, 255, color);
        event.effect = effect;

        // Configurer des valeurs par défaut pour les autres champs requis
        event.mask = 0xFF;
        event.target_uid = 0;

        // Temps relatifs par défaut (événement immédiat)
        LOGI("Configuration des temps");
        Relative_time_ms startTime(0);
        Relative_time_ms stopTime(1000);
        event.r_start_event_ms = startTime;
        event.r_stop_event_ms = stopTime;

        // Localisation par défaut (pas de GPS)
        LOGI("Configuration localisation");
        Localization localization;
        event.localization = localization;

        // Layer par défaut
        LOGI("Configuration layer");
        Layer layer(0, 255, Normal);
        event.layer = layer;

        // Encoder la trame
        LOGI("Encodage de l'événement");
        std::vector<uint8_t> payload = event.encode();

        LOGI("Event payload créé, taille: %zu octets", payload.size());

        // Log des premiers octets du payload pour debug
        if (payload.size() > 0) {
            std::string hexStr;
            for (size_t i = 0; i < std::min(payload.size(), size_t(16)); i++) {
                char buf[8];
                snprintf(buf, sizeof(buf), "0x%02x ", payload[i]);
                hexStr += buf;
            }
            LOGI("Premiers octets payload: %s", hexStr.c_str());
        }

        // Encapsuler le message avec le protocole
        std::vector<uint8_t> frame = encapsulateMessage(payload);

        LOGI("Event message encapsulé, taille totale: %zu octets", frame.size());

        // Log des premiers octets de la trame complète pour debug
        if (frame.size() > 0) {
            std::string hexStr;
            for (size_t i = 0; i < std::min(frame.size(), size_t(16)); i++) {
                char buf[8];
                snprintf(buf, sizeof(buf), "0x%02x ", frame[i]);
                hexStr += buf;
            }
            LOGI("Premiers octets trame: %s", hexStr.c_str());
        }

        // Convertir en jbyteArray
        jbyteArray result = env->NewByteArray(frame.size());
        env->SetByteArrayRegion(result, 0, frame.size(), reinterpret_cast<const jbyte*>(frame.data()));

        return result;
    } catch (const std::exception& e) {
        LOGE("Exception dans createEventMessage: %s", e.what());
        return nullptr;
    } catch (...) {
        LOGE("Exception inconnue dans createEventMessage");
        return nullptr;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_example_apptest2_wristband_WristbandNative_createCommandMessage(
        JNIEnv *env,
        jobject /* this */,
        jint command,
        jint param1,
        jint param2) {

    try {
        LOGI("Début création Command message - Cmd: %d, Param1: %d, Param2: %d", command, param1, param2);

        // Créer une commande avec la librairie wristband_objects
        WB_Command cmd;

        // Convertir la commande (vous devrez adapter selon votre enum WB_Command_type)
        WB_Command_type commandType = static_cast<WB_Command_type>(command);

        cmd.set_command(commandType, static_cast<uint32_t>(param1), static_cast<uint32_t>(param2));

        // Encoder la trame
        std::vector<uint8_t> payload = cmd.encode();

        LOGI("Command payload créé, taille: %zu octets", payload.size());

        // Encapsuler le message avec le protocole
        std::vector<uint8_t> frame = encapsulateMessage(payload);

        LOGI("Command message encapsulé, taille totale: %zu octets", frame.size());

        // Convertir en jbyteArray
        jbyteArray result = env->NewByteArray(frame.size());
        env->SetByteArrayRegion(result, 0, frame.size(), reinterpret_cast<const jbyte*>(frame.data()));

        return result;
    } catch (const std::exception& e) {
        LOGE("Exception dans createCommandMessage: %s", e.what());
        return nullptr;
    } catch (...) {
        LOGE("Exception inconnue dans createCommandMessage");
        return nullptr;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_apptest2_wristband_WristbandNative_validateFrame(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray frame) {

    jsize frameSize = env->GetArrayLength(frame);
    jbyte* frameData = env->GetByteArrayElements(frame, nullptr);

    // Convertir en std::vector
    std::vector<uint8_t> frameVector(
        reinterpret_cast<uint8_t*>(frameData),
        reinterpret_cast<uint8_t*>(frameData) + frameSize
    );

    // Validation basique - vérifier que la taille est raisonnable
    bool isValid = frameSize > 2 && frameSize <= 256;

    // Libérer les ressources JNI
    env->ReleaseByteArrayElements(frame, frameData, JNI_ABORT);

    return static_cast<jboolean>(isValid);
}

JNIEXPORT jstring JNICALL
Java_com_example_apptest2_wristband_WristbandNative_getFrameInfo(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray frame) {

    jsize frameSize = env->GetArrayLength(frame);
    jbyte* frameData = env->GetByteArrayElements(frame, nullptr);

    // Convertir en std::vector
    std::vector<uint8_t> frameVector(
        reinterpret_cast<uint8_t*>(frameData),
        reinterpret_cast<uint8_t*>(frameData) + frameSize
    );

    // Créer une chaîne d'information sur la trame
    std::string info = "Taille: " + std::to_string(frameSize) + " octets";

    if (frameSize >= 2) {
        uint16_t identifier = static_cast<uint16_t>(frameData[0]) |
                             (static_cast<uint16_t>(frameData[1]) << 8);
        info += ", ID: 0x" + std::to_string(identifier);
    }

    // Libérer les ressources JNI
    env->ReleaseByteArrayElements(frame, frameData, JNI_ABORT);

    return env->NewStringUTF(info.c_str());
}

// Nouvelle fonction pour createDetailedEventMessage avec TOUS les paramètres
JNIEXPORT jbyteArray JNICALL
Java_com_example_apptest2_wristband_WristbandNative_createDetailedEventMessage(
        JNIEnv *env,
        jobject /* this */,
        jlong rStartEventMs,
        jlong rStopEventMs,
        jint mask,
        jint styleValue,
        jint frequency,
        jint duration,
        jint intensity,
        jint colorRed,
        jint colorGreen,
        jint colorBlue,
        jint colorWhite,
        jint colorVibration,
        jint mapId,
        jint focus,
        jint zoom,
        jint goboTypeValue,
        jint layerNbr,
        jint layerOpacity,
        jint blendingModeValue) {

    try {
        LOGI("=== CRÉATION EVENT DÉTAILLÉ ===");
        LOGI("Timing: %lld-%lld ms, mask=%d", rStartEventMs, rStopEventMs, mask);
        LOGI("Style: %d, freq=%d Hz, dur=%d ms, int=%d", styleValue, frequency, duration, intensity);
        LOGI("Couleur RGBWV: (%d,%d,%d,%d,%d)", colorRed, colorGreen, colorBlue, colorWhite, colorVibration);
        LOGI("Localisation: map=%d, focus=%d, zoom=%d, gobo=%d", mapId, focus, zoom, goboTypeValue);
        LOGI("Layer: nbr=%d, opacity=%d, blend=%d", layerNbr, layerOpacity, blendingModeValue);

        // Créer un événement détaillé avec TOUS les paramètres
        Event event;

        // 1. Configuration de l'effet avec TOUS les paramètres couleur
        Style eventStyle = static_cast<Style>(styleValue);
        uint8_t color[NB_COLORS_V0_0] = {
            static_cast<uint8_t>(colorRed),      // Rouge
            static_cast<uint8_t>(colorGreen),    // Vert
            static_cast<uint8_t>(colorBlue),     // Bleu
            static_cast<uint8_t>(colorWhite),    // Blanc
            static_cast<uint8_t>(colorVibration) // Vibration
        };

        LOGI("Création Effect détaillé");
        Effect effect(eventStyle,
                     static_cast<uint8_t>(frequency),
                     static_cast<uint8_t>(duration),
                     static_cast<uint8_t>(intensity),
                     color);
        event.effect = effect;

        // 2. Configuration du masque et target
        event.mask = static_cast<uint8_t>(mask);
        event.target_uid = 0; // Par défaut

        // 3. Configuration des temps détaillés
        LOGI("Configuration temps détaillés");
        Relative_time_ms startTime(static_cast<uint32_t>(rStartEventMs));
        Relative_time_ms stopTime(static_cast<uint32_t>(rStopEventMs));
        event.r_start_event_ms = startTime;
        event.r_stop_event_ms = stopTime;

        // 4. Configuration de la localisation détaillée
        LOGI("Configuration localisation détaillée");
        Localization localization;
        localization.map_id = static_cast<uint8_t>(mapId);
        localization.focus = static_cast<uint8_t>(focus);    // Facteur de fade (0-255)
        localization.zoom = static_cast<uint8_t>(zoom);      // Profondeur d'effet (0-255)
        localization.gobo_type = static_cast<GOBO_Type>(goboTypeValue);
        event.localization = localization;

        // 5. Configuration du layer détaillé
        LOGI("Configuration layer détaillé");
        Blending_mode blendMode = static_cast<Blending_mode>(blendingModeValue);
        Layer layer(static_cast<uint8_t>(layerNbr),
                   static_cast<uint8_t>(layerOpacity),
                   blendMode);
        event.layer = layer;

        // 6. Encoder la trame
        LOGI("Encodage de l'événement détaillé");
        std::vector<uint8_t> payload = event.encode();

        LOGI("Event détaillé payload créé, taille: %zu octets", payload.size());

        // Log détaillé des premiers octets pour debug
        if (payload.size() > 0) {
            std::string hexStr;
            for (size_t i = 0; i < std::min(payload.size(), size_t(32)); i++) {
                char buf[8];
                snprintf(buf, sizeof(buf), "0x%02x ", payload[i]);
                hexStr += buf;
            }
            LOGI("Payload détaillé: %s%s", hexStr.c_str(), payload.size() > 32 ? "..." : "");
        }

        // 7. Encapsuler le message avec le protocole
        std::vector<uint8_t> frame = encapsulateMessage(payload);

        LOGI("Event détaillé encapsulé, taille totale: %zu octets", frame.size());
        LOGI("=== FIN CRÉATION EVENT DÉTAILLÉ ===");

        // 8. Convertir en jbyteArray
        jbyteArray result = env->NewByteArray(frame.size());
        env->SetByteArrayRegion(result, 0, frame.size(), reinterpret_cast<const jbyte*>(frame.data()));

        return result;
    } catch (const std::exception& e) {
        LOGE("Exception dans createDetailedEventMessage: %s", e.what());
        return nullptr;
    } catch (...) {
        LOGE("Exception inconnue dans createDetailedEventMessage");
        return nullptr;
    }
}

// Nouvelle fonction pour synchroniser l'heure du device avec le smartphone
JNIEXPORT jbyteArray JNICALL
Java_com_example_apptest2_wristband_WristbandNative_createTimeSyncMessage(
        JNIEnv *env,
        jobject /* this */) {

    try {
        LOGI("=== CRÉATION MESSAGE SYNCHRONISATION TEMPS ===");

        // Obtenir le temps actuel en microsecondes depuis l'epoch Unix
        auto now = std::chrono::system_clock::now();
        auto nowAsTimeT = std::chrono::system_clock::to_time_t(now);
        auto nowAsMicroseconds = std::chrono::duration_cast<std::chrono::microseconds>(now.time_since_epoch());

        LOGI("Temps actuel: %ld secondes depuis l'Epoch", nowAsTimeT);
        LOGI("Temps actuel: %lld microsecondes depuis l'Epoch", nowAsMicroseconds.count());

        // Créer un objet Absolute_time_us avec le temps actuel
        Absolute_time_us absolute_time_us(nowAsMicroseconds.count());

        // Encoder le message de temps
        std::vector<uint8_t> payload = absolute_time_us.encode();

        LOGI("Message temps créé, taille payload: %zu octets", payload.size());

        // Log des premiers octets pour debug
        if (payload.size() > 0) {
            std::string hexStr;
            for (size_t i = 0; i < std::min(payload.size(), size_t(16)); i++) {
                char buf[8];
                snprintf(buf, sizeof(buf), "0x%02x ", payload[i]);
                hexStr += buf;
            }
            LOGI("Payload temps: %s%s", hexStr.c_str(), payload.size() > 16 ? "..." : "");
        }

        // Encapsuler le message avec le protocole
        std::vector<uint8_t> frame = encapsulateMessage(payload);

        LOGI("Message temps encapsulé, taille totale: %zu octets", frame.size());
        LOGI("=== FIN CRÉATION MESSAGE SYNCHRONISATION TEMPS ===");

        // Convertir en jbyteArray
        jbyteArray result = env->NewByteArray(frame.size());
        env->SetByteArrayRegion(result, 0, frame.size(), reinterpret_cast<const jbyte*>(frame.data()));

        return result;
    } catch (const std::exception& e) {
        LOGE("Exception dans createTimeSyncMessage: %s", e.what());
        return nullptr;
    } catch (...) {
        LOGE("Exception inconnue dans createTimeSyncMessage");
        return nullptr;
    }
}

} // extern "C"

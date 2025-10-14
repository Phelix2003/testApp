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

} // extern "C"

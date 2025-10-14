# Intégration de la librairie wristband_objects

## Vue d'ensemble

Ce projet Android intègre la librairie C++ `wristband_objects` pour générer des trames de protocole wristband qui sont ensuite envoyées via USB CDC.

## Structure du projet

```
app/src/main/
├── cpp/                                    # Code natif C++
│   ├── CMakeLists.txt                     # Configuration CMake
│   ├── wristband_jni.cpp                  # Interface JNI
│   ├── libs/                              # Librairies externes
│   │   └── wristband_objects/             # Librairie wristband (sous-module Git)
│   ├── include/                           # Headers du projet
│   └── src/                               # Sources C++ du projet
│
└── java/com/example/apptest2/
    ├── MainActivity.kt                     # Interface utilisateur
    ├── wristband/
    │   └── WristbandNative.kt             # Wrapper Kotlin pour JNI
    └── usb/
        └── UsbManager.kt                   # Gestion USB CDC
```

## Fonctionnalités

### 1. Génération de trames Event

La classe `Event` de la librairie wristband_objects est utilisée pour créer des trames de protocole :

- **Event simple** : Avec couleur et style prédéfinis
- **Event complet** : Avec tous les paramètres (fréquence, durée, intensité, temps, etc.)

### 2. Styles disponibles

```kotlin
enum class WristbandStyle {
    ON, OFF, STROBE, WAVE, PW, HEARTBEAT, 
    SPARKLE, PULSE, RANDOM, ACC_ON, ACC_ON_XYZ, 
    ACC_CLAP, ACC_POSITION, ACC_FLASH, CLEAR, 
    ACC_HOLO, BOOM
}
```

### 3. Couleurs

Chaque trame peut contenir 5 composantes de couleur :
- Rouge (0-255)
- Vert (0-255)
- Bleu (0-255)
- Blanc (0-255)
- Vibration (0-255)

## Utilisation

### Initialisation

```kotlin
val wristbandFrameManager = WristbandFrameManager()
wristbandFrameManager.initialize()
```

### Génération d'une trame simple

```kotlin
val color = WristbandColor(red = 255, green = 0, blue = 0)
val style = WristbandStyle.STROBE
val frame = wristbandFrameManager.generateSimpleEvent(style, color)
```

### Génération d'une trame complète

```kotlin
val color = WristbandColor(red = 0, green = 255, blue = 0)
val frame = wristbandFrameManager.generateCompleteEvent(
    style = WristbandStyle.PULSE,
    color = color,
    frequency = 15,
    duration = 200,
    intensity = 200,
    startTimeMs = 0,
    stopTimeMs = 5000
)
```

### Envoi via USB CDC

```kotlin
val usbCdcManager = UsbCdcManager(context)
val success = usbCdcManager.sendBytes(frame)
```

### Debug

```kotlin
// Convertir la trame en hexadécimal pour le debug
val hexString = wristbandFrameManager.frameToHexString(frame)
// Exemple : "A5 01 FF 00 00 ..."
```

## Architecture technique

### Couche C++ (JNI)

Le fichier `wristband_jni.cpp` expose les fonctions natives :

1. **initialize()** : Initialise la librairie wristband_objects
2. **generateSimpleEventFrame()** : Génère une trame Event simple
3. **generateEventFrame()** : Génère une trame Event complète
4. **validateFrame()** : Valide une trame
5. **parseFrame()** : Parse une trame reçue

### Couche Kotlin

- **WristbandNative** : Interface JNI native (external functions)
- **WristbandFrameManager** : Gestionnaire de haut niveau avec API simplifiée
- **WristbandStyle** : Énumération des styles disponibles
- **WristbandColor** : Classe de données pour les couleurs

### Flux de données

```
Kotlin (MainActivity)
    ↓
WristbandFrameManager.generateSimpleEvent()
    ↓
WristbandNative.generateSimpleEventFrame() [JNI]
    ↓
Event.encode() [C++]
    ↓
ByteArray (trame encodée)
    ↓
UsbCdcManager.sendBytes()
    ↓
USB CDC (périphérique)
```

## Compilation

### Prérequis

- Android Studio
- NDK installé
- CMake 3.18.1+
- Git (pour les sous-modules)

### Build

```bash
./gradlew build
```

Le NDK compilera automatiquement la librairie C++ et créera `libwristband_native.so` pour chaque architecture (arm64-v8a, armeabi-v7a, x86, x86_64).

## Débogage

### Logs natifs

Les logs C++ apparaissent dans Logcat avec le tag `WristbandNative` :

```
I/WristbandNative: 🔧 Génération d'une trame Event simple (ID: 1, Style: 2)
I/WristbandNative: ✅ Trame simple générée: 45 octets
```

### Logs USB

Les logs USB apparaissent dans l'interface avec des informations détaillées sur l'envoi.

## Boutons de test

L'interface propose plusieurs boutons pour tester :

- **Event CMD** : Trame ON rouge
- **Event DATA** : Trame STROBE verte
- **Event STATUS** : Trame PULSE bleue
- **Event ACK** : Trame HEARTBEAT blanche

## Dépannage

### La librairie native ne se charge pas

Vérifiez que :
- Le sous-module `wristband_objects` est bien présent dans `app/src/main/cpp/libs/`
- Le build Gradle a réussi à compiler le code C++
- Les fichiers `.so` sont présents dans l'APK

### Erreurs de compilation C++

- Vérifiez que tous les headers de `wristband_objects` sont accessibles
- Assurez-vous que le standard C++17 est bien configuré dans CMakeLists.txt

### La trame n'est pas envoyée

- Vérifiez qu'un périphérique USB CDC est connecté
- Accordez les permissions USB quand demandé
- Consultez les logs USB pour plus de détails

## Prochaines étapes

- [ ] Ajouter la réception de trames via USB CDC
- [ ] Implémenter le parsing complet des trames reçues
- [ ] Ajouter le support des GPS maps
- [ ] Créer des presets de trames pour différents effets
- [ ] Ajouter une interface pour configurer tous les paramètres de l'Event

## Ressources

- Librairie wristband_objects : https://github.com/vibz-it/wristband_objects
- Documentation Android NDK : https://developer.android.com/ndk
- USB CDC Android : https://developer.android.com/guide/topics/connectivity/usb


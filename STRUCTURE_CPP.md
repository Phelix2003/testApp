# Structure recommandée pour app/src/main/cpp/

cpp/
├── CMakeLists.txt                 # Configuration de build principale
├── wristband_jni.cpp             # Interface JNI (pont Java/Kotlin ↔ C++)
├── libs/                         # Dossier pour les librairies externes
│   └── wristband_objects/        # La librairie wristband_objects ici
│       ├── include/              # Headers publics de la librairie
│       ├── src/                  # Sources de la librairie
│       └── CMakeLists.txt        # Configuration build de la librairie (si elle en a une)
├── include/                      # Headers spécifiques à votre projet
│   └── wristband_wrapper.h       # Headers pour vos wrappers C++
└── src/                          # Sources C++ spécifiques à votre projet
    └── wristband_wrapper.cpp     # Implémentations de vos wrappers

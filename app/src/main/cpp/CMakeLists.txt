# CMakeLists.txt pour la bibliothèque wristband_native

cmake_minimum_required(VERSION 3.22.1)

# Nom du projet
project("wristband_native")

# Standard C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Inclure les headers de la librairie wristband_objects
include_directories(${CMAKE_SOURCE_DIR}/wristband_obje)

# Sources de la librairie wristband_objects (tous dans une seule lib pour Android)
set(WRISTBAND_SOURCES
    wristband_obje/control.cpp
    wristband_obje/Event.cpp
    wristband_obje/BufferUtils.cpp
    wristband_obje/CoordinatesRef.cpp
    wristband_obje/OTA_flash_message.cpp
    wristband_obje/RCoordinates.cpp
    wristband_obje/event_lib.cpp
    wristband_obje/rtime.cpp
    wristband_obje/sys_cfg.cpp
)

# Fichier JNI wrapper
set(JNI_WRAPPER_SOURCES
    wristband_jni.cpp
)

# Créer la bibliothèque partagée unique pour Android
add_library(
    wristband_native
    SHARED
    ${WRISTBAND_SOURCES}
    ${JNI_WRAPPER_SOURCES}
)

# Configurer les propriétés de compilation
set_target_properties(wristband_native PROPERTIES
    POSITION_INDEPENDENT_CODE ON
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
)

# Lier avec les bibliothèques Android nécessaires
find_library(log-lib log)

target_link_libraries(
    wristband_native
    ${log-lib}
)

# Définir les flags de compilation
target_compile_definitions(wristband_native PRIVATE
    ANDROID_BUILD=1
    _GNU_SOURCE=1
)

# Optimisations et flags spécifiques
target_compile_options(wristband_native PRIVATE
    -Wall
    -Wextra
    -fPIC
)

# Optimisations pour la release
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(wristband_native PRIVATE -O3 -DNDEBUG)
else()
    target_compile_options(wristband_native PRIVATE -O0 -g)
endif()

#!/bin/bash

# Script pour télécharger et intégrer la librairie wristband_objects
echo "Téléchargement de la librairie wristband_objects..."

# Aller dans le dossier cpp
cd app/src/main/cpp

# Cloner la librairie si elle n'existe pas déjà
if [ ! -d "wristband_objects" ]; then
    git clone https://github.com/vibz-it/wristband_objects.git
    echo "Librairie téléchargée avec succès"
else
    echo "La librairie existe déjà, mise à jour..."
    cd wristband_objects
    git pull
    cd ..
fi

echo "Intégration terminée. La librairie est prête à être utilisée."
echo "Vous pouvez maintenant compiler le projet avec './gradlew build'"

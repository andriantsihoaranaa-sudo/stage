#!/usr/bin/env bash
# Script de lancement de Gestion des Naissances sous Linux / macOS
set -e

echo "=== Lancement de « Gestion des Naissances » ==="
if command -v mvn &> /dev/null; then
    mvn javafx:run
else
    echo "Maven n'est pas détecté dans le PATH. Veuillez installer Maven ou exécuter le JAR compilé."
    if [ -f "target/gestion-naissances-1.0.0.jar" ]; then
        java -jar target/gestion-naissances-1.0.0.jar
    fi
fi

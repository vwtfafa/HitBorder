#!/bin/bash
# HitBorder v1.0 - Installation Script (Linux/Mac)

echo ""
echo "========================================"
echo "  HitBorder v1.0 - Installation"
echo "  Minecraft 1.21.10 Paper Plugin"
echo "========================================"
echo ""

# Prüfe ob JAR existiert
if [ ! -f "HitBorder-1.0.jar" ]; then
    echo "[ERROR] HitBorder-1.0.jar nicht gefunden!"
    echo "Bitte stelle sicher, dass diese Datei im gleichen Verzeichnis ist."
    exit 1
fi

# Suche Paper-Server
SERVER_PATH="./server"
if [ ! -d "$SERVER_PATH" ]; then
    SERVER_PATH="../server"
fi
if [ ! -d "$SERVER_PATH" ]; then
    SERVER_PATH="$HOME/Paper/server"
fi

if [ ! -d "$SERVER_PATH" ]; then
    echo "[INFO] Paper-Server-Verzeichnis nicht automatisch gefunden."
    read -p "Bitte Paper-Server-Pfad eingeben: " SERVER_PATH
fi

# Prüfe ob Server existiert
if [ ! -d "$SERVER_PATH" ]; then
    echo "[ERROR] Server-Verzeichnis nicht gefunden: $SERVER_PATH"
    exit 1
fi

# Prüfe ob plugins-Ordner existiert
if [ ! -d "$SERVER_PATH/plugins" ]; then
    echo "[INFO] plugins-Ordner existiert noch nicht. Erstelle..."
    mkdir -p "$SERVER_PATH/plugins"
fi

# Kopiere JAR
echo "[INFO] Kopiere HitBorder-1.0.jar nach plugins-Verzeichnis..."
cp "HitBorder-1.0.jar" "$SERVER_PATH/plugins/HitBorder-1.0.jar"

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "  [SUCCESS] Installation abgeschlossen!"
    echo "========================================"
    echo ""
    echo "Nächste Schritte:"
    echo "1. Starte Paper-Server: java -jar paper-1.21.10.jar nogui"
    echo "2. Plugin wird automatisch geladen"
    echo "3. Bearbeite config: plugins/HitBorder/config.yml"
    echo "4. Laden Sie neu: /reload confirm"
    echo ""
    echo "Teste das Plugin:"
    echo "- /hitborder status"
    echo "- /hitborder help"
    echo ""
else
    echo ""
    echo "[ERROR] Installation fehlgeschlagen!"
    exit 1
fi


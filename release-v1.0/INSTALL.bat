@echo off
REM HitBorder v1.0 - Installation Script (Windows)

echo.
echo ========================================
echo   HitBorder v1.0 - Installation
echo   Minecraft 1.21.10 Paper Plugin
echo ========================================
echo.

REM Prüfe ob JAR existiert
if not exist "HitBorder-1.0.jar" (
    echo [ERROR] HitBorder-1.0.jar nicht gefunden!
    echo Bitte stelle sicher, dass diese Datei im gleichen Verzeichnis ist.
    pause
    exit /b 1
)

REM Suche Paper-Server in üblichen Verzeichnissen
set SERVER_PATH=
if exist "server" (
    set SERVER_PATH=server
) else if exist "..\server" (
    set SERVER_PATH=..\server
) else if exist "C:\Paper\server" (
    set SERVER_PATH=C:\Paper\server
) else (
    echo [INFO] Paper-Server-Verzeichnis nicht automatisch gefunden.
    set /p SERVER_PATH="Bitte Paper-Server-Pfad eingeben: "
)

REM Prüfe ob Server existiert
if not exist "%SERVER_PATH%" (
    echo [ERROR] Server-Verzeichnis nicht gefunden: %SERVER_PATH%
    pause
    exit /b 1
)

REM Prüfe ob plugins-Ordner existiert
if not exist "%SERVER_PATH%\plugins" (
    echo [INFO] plugins-Ordner existiert noch nicht. Erstelle...
    mkdir "%SERVER_PATH%\plugins"
)

REM Kopiere JAR
echo [INFO] Kopiere HitBorder-1.0.jar nach plugins-Verzeichnis...
copy "HitBorder-1.0.jar" "%SERVER_PATH%\plugins\HitBorder-1.0.jar"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   [SUCCESS] Installation abgeschlossen!
    echo ========================================
    echo.
    echo Nächste Schritte:
    echo 1. Starte Paper-Server: java -jar paper-1.21.10.jar nogui
    echo 2. Plugin wird automatisch geladen
    echo 3. Bearbeite config: plugins/HitBorder/config.yml
    echo 4. Laden Sie neu: /reload confirm
    echo.
    echo Teste das Plugin:
    echo - /hitborder status
    echo - /hitborder help
    echo.
    pause
) else (
    echo.
    echo [ERROR] Installation fehlgeschlagen!
    pause
    exit /b 1
)


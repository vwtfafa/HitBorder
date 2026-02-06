HitBorder
========

Kurze Beschreibung

Ein simples Paper/Spigot-Plugin für Minecraft 1.21.11: Wenn ein Spieler Schaden nimmt, wächst die Weltgrenze (WorldBorder).

Schnellstart

1. Builden: Öffne ein Terminal im Projekt-Root und führe aus:

   ./gradlew.bat clean shadowJar

   Ergebnis: build/libs/Hit-Border-<version>.jar

2. Kopiere die JAR in dein Paper-Server `plugins/`-Verzeichnis (Paper 1.21.11) und starte den Server mit einer kompatiblen Java-Version (siehe unten).

3. Testen: Betritt die Welt (`/world`), erzeuge Schaden (z.B. /damage oder durch Mobs), und beobachte, wie sich die WorldBorder vergrößert.

Wichtig

- Server-Java: Dieses Projekt ist für Java 21 kompiliert (Gradle Toolchain). Stelle sicher, dass dein Server Java 21 oder kompatibel ausführt. Wenn du Ziel-Java ändern möchtest, passe `build.gradle` an.
- API-Version: `plugin.yml` verwendet `api-version: '1.21'`, passend für Minecraft 1.21.x.
- ShadowJar: Abhängigkeiten wie bStats und Kyori Adventure werden in das Shadow-JAR verfrachtet, um Kompatibilitätsprobleme zu vermeiden.

Konfiguration

Die Standard-Konfiguration befindet sich in `src/main/resources/config.yml`. Wichtige Keys:
- `border.initial-size` (Radius)
- `border.grow-amount` (Radius pro Treffer)
- `game.hardcore` (wenn true töten Spieler beim Erreichen des Maximalwertes)

Weiteres

Wenn du möchtest, kann ich:
- fehlende Nachrichten-Keys ergänzen und Standardtexte setzen
- eine Release-Checklist / GitHub Actions für automatische Builds erstellen
- Kompatibilität mit Java 17 (falls benötigt) herstellen

Versionierung

- Die Projektversion wird aus der Datei `VERSION` gelesen. Ändere dort die Versionsnummer und das Build/Release übernimmt sie automatisch.
- Releases starten automatisch bei Tag-Pushes (`beta`, `release`, `relse`) oder manuell über GitHub Actions → „Release Build“ (Input `channel`).

Lizenz: Siehe LICENSE (falls vorhanden im Repo).

# Einkaufsliste

Eine native Android-App für die Einkaufsliste — offline, ohne Konto, ohne Werbung.
Gebaut mit Kotlin, Jetpack Compose und Material 3 (Material You), getestet auf
Kompatibilität mit Pixel-Geräten (Android 8.0 und neuer).

## Was die App kann

- **Artikel erfassen** mit Name, optionaler Menge („500 g", „2 Packungen") und Kategorie
- **Abhaken** — erledigte Artikel rutschen durchgestrichen in einen eigenen Abschnitt
- **Nach Supermarkt-Kategorien gruppiert** (Obst & Gemüse → Backwaren → Molkerei → …),
  damit die Liste der Route durch den Laden folgt
- **Bearbeiten** durch Antippen eines Eintrags
- **Löschen** einzeln, „erledigte entfernen" oder Liste leeren — jeweils mit
  **Rückgängig** direkt im Snackbar
- **Material You**: übernimmt auf Android 12+ die Farben deines Hintergrundbilds,
  inklusive Dark Mode
- **Alles lokal** in einer Room-Datenbank; keine Netzwerkberechtigung im Manifest

## APK aufs Pixel bekommen

Es gibt zwei Wege — der erste braucht keine Entwicklungsumgebung.

### 1. Fertige APK aus GitHub Actions (einfachster Weg)

1. Im Repository auf **Actions** → Workflow **Android** → den neuesten grünen Lauf öffnen.
2. Unter **Artifacts** `einkaufsliste-debug-apk` herunterladen und entpacken.
3. Die `app-debug.apk` aufs Pixel kopieren (USB, Google Drive, Nearby Share …).
4. Auf dem Pixel öffnen. Android fragt einmal nach der Erlaubnis
   „Unbekannte Apps installieren" für die App, aus der du die Datei öffnest
   (z. B. Files oder Chrome) — bestätigen, dann installieren.

Die Debug-APK ist mit einem Debug-Schlüssel signiert. Das reicht zum Selbstnutzen;
für den Play Store bräuchte es einen eigenen Release-Keystore.

### 2. Aus Android Studio

```bash
git clone https://github.com/CodingKody99/claude.git
cd claude
```

Ordner in Android Studio öffnen (Ladybug oder neuer), Pixel per USB mit aktiviertem
USB-Debugging verbinden und auf **Run** drücken. Oder per Kommandozeile:

```bash
./gradlew installDebug      # baut und installiert auf dem verbundenen Gerät
./gradlew assembleDebug     # baut nur die APK
./gradlew testDebugUnitTest # Unit-Tests
```

Dafür wird ein Android SDK mit API-Level 34 benötigt (`ANDROID_HOME` gesetzt oder
`local.properties` mit `sdk.dir=…`).

## Aufbau

```
app/src/main/java/io/github/codingkody99/einkaufsliste/
├── EinkaufslisteApplication.kt   Datenbank + Repository als Singletons
├── MainActivity.kt               Einstiegspunkt, setzt Theme und Screen
├── data/                         Room: Entity, DAO, Converter, Repository
│   ├── Category.kt               Kategorien; Reihenfolge = Sortierung im UI
│   ├── ShoppingItem.kt
│   ├── ShoppingDao.kt
│   ├── AppDatabase.kt
│   └── ShoppingRepository.kt     Interface + Room-Implementierung
├── domain/
│   ├── ShoppingListRow.kt        Kopfzeile oder Eintrag
│   └── ShoppingListGrouper.kt    Gruppierung & Sortierung (reines Kotlin)
└── ui/
    ├── ShoppingListUiState.kt
    ├── ShoppingListViewModel.kt
    ├── ShoppingListScreen.kt
    ├── ItemEditorSheet.kt
    └── theme/
```

Die Sortier- und Gruppierlogik liegt bewusst in `domain/` und kennt weder Android
noch Room — dadurch ist sie ohne Emulator testbar. Das `ShoppingRepository` ist ein
Interface, sodass das ViewModel gegen einen In-Memory-Fake getestet wird.

## Tests

Reine JVM-Unit-Tests, kein Emulator nötig:

```bash
./gradlew testDebugUnitTest
```

Abgedeckt sind die Gruppierung und Sortierung der Liste, die Kategorie-Konvertierung
für Room, das Repository (Trimmen, leere Eingaben, Wiederherstellen) und das
ViewModel (Editor, Abhaken, Löschen mit Rückgängig).

## Technik

| | |
|---|---|
| Sprache | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 (Compose BOM 2024.09.00) |
| Architektur | MVVM — ViewModel + `StateFlow`, Repository-Interface |
| Persistenz | Room 2.6.1 (KSP) |
| Build | Gradle 8.9, Android Gradle Plugin 8.5.2, JDK 17 |
| minSdk / targetSdk | 26 / 34 |

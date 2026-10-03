# Einkaufsliste

Eine native Android-App für die Einkaufsliste — offline, ohne Konto, ohne Werbung.
Gebaut mit Kotlin, Jetpack Compose und Material 3 (Material You), getestet auf
Kompatibilität mit Pixel-Geräten (Android 8.0 und neuer).

## Was die App kann

- **Freitext einfügen**: oben auf „Ganze Liste einfügen" tippen, beliebigen Text
  hineinwerfen — Stichpunktliste, Komma-Liste, Rezept nach Gerichten — und die App
  zerlegt ihn in einzelne Artikel, erkennt Mengen und sortiert alles nach
  Supermarkt-Reihenfolge. Vor dem Übernehmen gibt es eine **Vorschau**, in der jede
  Zeile abgewählt oder umsortiert werden kann.
- **Diktieren**: statt zu tippen einfach sagen, was gebraucht wird — „Ich würde
  gerne Sommerrollen machen und brauche dafür Karotten, einen Tofu eine Gurke,
  Reispapier Reisnudeln" wird zum Gericht als Überschrift plus fünf einsortierten
  Artikeln. Satzfüller fallen weg, und da Spracherkennung keine Kommas setzt,
  werden auch Wortketten ohne Satzzeichen in einzelne Artikel zerlegt
- **Rezeptbuch** über das 🍳-Symbol oben: Rezepte eintippen, per Link laden,
  diktieren oder **vom Foto scannen** — für Rezepte auf Papier. Dazu ein Feld für
  den Ablauf. Ein Tipp auf ein Rezept öffnet es zum Lesen; von dort gehen die
  Zutaten über dieselbe Vorschau wie der Freitext-Import auf die aktuelle Liste
- **Rezept-Link einfügen**: statt Text einen Link zu einer Rezeptseite einwerfen —
  die Zutatenliste wird geladen und läuft durch dieselbe Erkennung und dieselbe
  Vorschau. Rezeptangaben wie „3 Ei(er)", „etwas Butter zum Braten" oder
  „Mehl (Type 405)" werden dabei auf das reduziert, was man im Laden braucht.
- **Automatische Kategorisierung**: „Tomaten" landet in Obst & Gemüse, „Vollkornbrot"
  in Backwaren, „TK-Spinat" im Tiefkühler. Ohne Netz, ohne Konto.
- **Lernt mit**: jede Kategorie, die du von Hand korrigierst, gilt ab dann für
  diesen Artikel — auch für Plural und andere Schreibweisen.
- **Nach Supermarkt-Route gruppiert** (Obst & Gemüse → Backwaren → Molkerei →
  Fleisch → Tiefkühl → Vorrat → Süßes → Getränke → Haushalt), damit die Liste dem
  Weg durch den Laden folgt
- **Abhaken** — erledigte Artikel rutschen durchgestrichen in einen eigenen Abschnitt
- **Bearbeiten** durch Antippen eines Eintrags
- **Löschen** einzeln, „erledigte entfernen" oder Liste leeren — jeweils mit
  **Rückgängig**; auch ein kompletter Import lässt sich in einem Zug zurücknehmen
- **Material You**: übernimmt auf Android 12+ die Farben deines Hintergrundbilds,
  inklusive Dark Mode
- **Mehrere Listen**: die App startet immer auf der Hauptliste; über den Namen oben
  wechselt man zu anderen Listen, legt neue an, benennt um und löscht
- **Schnelleingabe unten**: das Feld ist immer da — antippen, Artikel eintippen,
  Enter. Die erkannte Kategorie steht darüber; ein Tipp darauf öffnet Menge und
  Kategorie
- **Alles lokal** in einer Room-Datenbank

### Wie aus Freitext eine sortierte Liste wird

Eingefügt:

```
Salat mit Käse
- 2 Tomaten
- Feta
- Olivenöl

Abendessen Freitag: Lachs, Kartoffeln
500g Hackfleisch
```

Ergebnis nach dem Übernehmen:

| Abschnitt | Artikel |
|---|---|
| 🥕 Obst & Gemüse | Salat, 2 Tomaten, Kartoffeln |
| 🧈 Molkerei & Kühlregal | Käse, Feta |
| 🥩 Fleisch, Wurst & Fisch | Lachs, 500 g Hackfleisch |
| 🍝 Vorrat & Konserven | Olivenöl |

„Salat mit Käse" wird in zwei Artikel zerlegt, weil auf einer Einkaufsliste beides
gebraucht wird — und beides in verschiedenen Gängen liegt. „Abendessen Freitag:"
wird als Überschrift erkannt und nicht übernommen (in der Vorschau sichtbar, dort
auch nachträglich einschaltbar).

### Wie aus einem Rezept-Link eine Einkaufsliste wird

Rezeptseiten veröffentlichen ihre Rezepte als **schema.org-Daten** im Seitenquelltext
(`recipeIngredient` in einem `application/ld+json`-Block) — das ist der Standard, der
Googles Rezept-Ergebnisse speist, und deshalb liefert ihn praktisch jede große Seite.
Die App liest genau diese Daten, statt die sichtbare Seite zu scrapen: maschinell
gedacht, stabil, und unabhängig davon wie die Seite gestaltet ist. Als Rückfalloption
werden auch `itemprop`-Microdata gelesen.

Findet die App keine Rezeptdaten, sagt sie das klar und schlägt vor, die Zutaten
stattdessen als Text einzufügen — statt stillschweigend eine leere Liste zu
übernehmen.

Die **Netzwerkberechtigung** wird ausschließlich dafür benutzt: die App ruft nur
Seiten auf, deren Link du selbst eingefügt hast. Keine Analytics, keine Konten,
keine Synchronisierung.

### Warum kein KI-Dienst für die Erkennung

Die Kategorisierung läuft vollständig im Gerät: ein deutsches Lebensmittel-Lexikon
plus Regeln für Komposita (`Hafermilch` → `milch`, `Vollkornbrot` → `brot`) und
Tiefkühl-Marker (`TK-Spinat`). Ein Cloud-Dienst hätte bedeutet: ein API-Key in der
APK, Kosten pro Nutzung und keine Funktion ohne Netz — ausgerechnet im Supermarkt.
Was das Lexikon nicht kennt, landet sichtbar in „Sonstiges" und wird beim ersten
Korrigieren gelernt.

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
├── data/                         Room
│   ├── Category.kt               Kategorien; Reihenfolge = Route im Supermarkt
│   ├── ShoppingItem.kt           gehört zu genau einer Liste
│   ├── ShoppingList.kt           die Listen selbst
│   ├── Recipe.kt                 gespeicherte Rezepte
│   ├── CategoryOverride.kt       gelernte Korrekturen
│   ├── ShoppingDao.kt / ShoppingListDao.kt / RecipeDao.kt / CategoryOverrideDao.kt
│   ├── AppDatabase.kt            v4, mit Migrationen für Lernen, Listen, Rezepte
│   └── ShoppingRepository.kt     Interface + Room-Implementierung
│   └── RecipeFetcher.kt          lädt eine Rezeptseite (nur HttpURLConnection)
├── domain/                       reines Kotlin, ohne Android und ohne Room
│   ├── TextNormalizer.kt         Umlaute, Groß-/Kleinschreibung, Plural
│   ├── FoodLexicon.kt            Lebensmittel-Vokabular je Kategorie
│   ├── CategoryClassifier.kt     Zuordnung inkl. Kompositum- und TK-Regeln
│   ├── QuantityParser.kt         „500g", „2x", „1/2", „Milch 1 l"
│   ├── ShoppingListParser.kt     Freitext → Artikel
│   ├── ItemSegmenter.kt          Wortketten ohne Satzzeichen trennen
│   ├── UrlDetector.kt            findet einen Link im eingefügten Text
│   ├── HtmlText.kt               HTML-Entities und Tags
│   ├── RecipeExtractor.kt        schema.org-Zutaten aus einer Seite
│   ├── ShoppingListRow.kt
│   └── ShoppingListGrouper.kt    Gruppierung & Sortierung
└── ui/
    ├── ShoppingListUiState.kt
    ├── ShoppingListViewModel.kt
    ├── ShoppingListViewModelFactory.kt
    ├── ShoppingListScreen.kt     Liste, Einfügefeld oben, Schnelleingabe unten
    ├── ListSwitcherSheet.kt      Listen wechseln, anlegen, umbenennen
    ├── RecipesSheet.kt           gespeicherte Rezepte
    ├── RecipeEditorSheet.kt      Rezept eintippen, laden oder diktieren
    ├── RecipeViewSheet.kt        Rezept lesen
    ├── DictateButton.kt          Spracheingabe über den System-Erkenner
    ├── ScanPhotoButton.kt        Texterkennung im Gerät (ML Kit)
    ├── ImportSheet.kt            Einfügen und Vorschau
    ├── ItemEditorSheet.kt
    └── theme/                    Farben und Typografie
```

Alles, was Entscheidungen trifft — Normalisierung, Lexikon, Zuordnung, Mengen,
Freitext-Zerlegung, Gruppierung — liegt in `domain/` und kennt weder Android noch
Room. Dadurch ist es ohne Emulator testbar. `ShoppingRepository` ist ein Interface,
das ViewModel wird gegen In-Memory-Fakes geprüft.

## Tests

Reine JVM-Unit-Tests, kein Emulator nötig:

```bash
./gradlew testDebugUnitTest
```

Abgedeckt sind unter anderem:

- **Normalisierung**: Umlaute, Schreibweisen, deutsche Pluralformen
- **Lexikon-Integrität**: kein Stichwort in zwei Kategorien, jedes Stichwort
  klassifiziert zurück in seine eigene Kategorie
- **Zuordnung**: Singular/Plural, Komposita (`Vollkornbrot`, `Hafermilch`),
  Wortgruppen (`Frischkäse Kräuter` ist Molkerei, nicht Kräuter),
  Tiefkühl-Marker, gelernte Korrekturen, und dass Unbekanntes *nicht* geraten wird
- **Mengen**: vorne, hinten, Brüche, Einheiten, reine Zahlen ohne Artikel
- **Freitext**: Stichpunkte, Nummerierung, Komma- und „und"/„mit"-Listen,
  Überschriften, `Milch: 1 l` gegen `Abendessen: Lachs, Dill`, und eine komplette
  Rezept-Liste am Stück
- **Import-Ablauf**: Vorschau in Supermarkt-Reihenfolge, Duplikaterkennung,
  Ab-/Anwählen, Umkategorisieren samt Lernen, Übernehmen und Rückgängig
- **Mehrere Listen**: Hauptliste beim Start (nicht die zuletzt benutzte), Anlegen,
  Umbenennen, Löschen samt Artikeln, Zählerstände je Liste, und dass die letzte
  Liste nicht gelöscht werden kann
- **Schnelleingabe**: Kategorie beim Tippen, Hinzufügen und Leeren des Felds,
  Übergabe des Getippten an den vollen Editor
- **Gesprochene Sätze**: Satzfüller („und dann brauche ich noch"), Gericht-Sätze
  („ich will X kochen" gegen „ich will Milch", und „Butter zum Braten" bleibt ein
  Artikel), gesprochene Zahlwörter, und dass Artikelnamen wie „Brauner Zucker"
  nicht für Füllwörter gehalten werden
- **Wortketten ohne Satzzeichen**: „einen Tofu eine Gurke" wird getrennt,
  „Rote Bete" und „Brauner Zucker" nicht, und ein nachgestelltes „Milch 1 l"
  verliert seine Menge nicht
- **Rezeptbuch**: Speichern, Bearbeiten, Löschen, Laden aus einem Link,
  Diktieren mit Name aus dem Gericht, Übernahme auf die Liste samt
  Duplikatprüfung — und dass ein Rezept bei jeder Nutzung neu eingeordnet wird,
  also von später Gelerntem profitiert
- **Rezept-Links**: Linkerkennung im eingefügten Text, Zutaten aus JSON-LD
  (auch verschachtelt in `@graph`, `@type` als Liste, Zutat als einzelner String),
  Microdata-Rückfall, defektes JSON, Seite ohne Rezept, Ladefehler — und ein
  komplettes Pfannkuchen-Rezept von der Seite bis zur sortierten Liste

## Technik

| | |
|---|---|
| Sprache | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 (Compose BOM 2024.09.00) |
| Architektur | MVVM — ViewModel + `StateFlow`, Repository-Interface |
| Persistenz | Room 2.6.1 (KSP), Schema v2 mit Migration |
| Netzwerk | `HttpURLConnection` (JDK), nur für eingefügte Rezept-Links |
| JSON | kotlinx.serialization 1.7.3 (nur Laufzeit, kein Compiler-Plugin) |
| Build | Gradle 8.9, Android Gradle Plugin 8.5.2, JDK 17 |
| minSdk / targetSdk | 26 / 34 |
| Spracheingabe | `RecognizerIntent` des Systems, ohne Mikrofon-Berechtigung |
| Texterkennung | ML Kit Text Recognition, auf dem Gerät, ohne Netz |

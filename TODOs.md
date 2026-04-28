# 📝 TODOs – VEBO Lagersystem

> Zuletzt aktualisiert: 01.04.2026

---

## 🐛 Bugfixes / Code-Qualität

- [x] `Main.java` Z.444 – JavaDoc-Parameter `progressListener` ist undefiniert → JavaDoc korrigieren
- [x] `Main.java` Z.262 – `current.getClass() != null` ist immer `true` → Bedingung entfernen oder sinnvoll ersetzen
- [x] `Main.java` Z.1196 – `channelResult == null` ist immer `false` → Nullcheck entfernen
- [x] Alle `System.err.println` in `SettingsDataTransferService`, `SettingsGUI`, `SettingsRuntimeService`, `WarningManager` und `Settings` durch **Log4j2-Logger** ersetzen
- [x] Alle `System.out.println` in `Main.java` (Startup-Banner etc.) durch **Log4j2-Logger** ersetzen
- [x] `ScanServer.java` – `System.out.println` / `System.err.println` durch Logger ersetzen
- [x] **GitHub-Token-Platzhalter**: `loadGitHubToken()` und `UpdateManager`-Konstruktor setzen den Token auch wenn er noch `"your_github_token_here"` ist → Platzhalter-Check vor `setPersonalToken()` ergänzen
- [x] **Doppelte Token-Initialisierung**: `UpdateManager()`-Konstruktor liest `github-token` aus Settings UND `Main.loadGitHubToken()` ruft danach erneut `setPersonalToken()` auf → einen der beiden Pfade entfernen
- [x] **`notes.content` Spalten-Typ**: `VARCHAR(2555)` in `NotesManager` und `DatabaseManager` – `2555` ist wahrscheinlich ein Tippfehler; für SQLite besser `TEXT` verwenden
- [x] **`catch (Exception e)` zu weit gefasst**: in `QRCodeUtils`, `ArticleUtils`, `WarningManager`, `ImportUtils`, `OrderExport` und `NetUtils` sollten spezifische Exception-Typen gefangen werden statt der Basis-Klasse
- [x] **`SimpleDateFormat` nicht thread-safe**: in `NewOrderGUI`, `MainGUI`, `LogsGUI` und `ArticleQrPreviewDialog` wird `SimpleDateFormat` verwendet → durch `DateTimeFormatter` + `LocalDate`/`LocalDateTime` ersetzen (wie bereits in `ScanServer` und `LogManager` getan)
- [x] **`OrderManager.extracted()` – nichtssagender Methodenname**: `private static String extracted(...)` → umbenennen in `serializeArticleEntry()`
- [x] **`OrderManager` fehlt `resetInstance()`**: alle anderen Manager haben diese Methode für Test-Reset; `OrderManager` fehlt sie → Tests für `OrderManager` sind dadurch aufwändiger
- [x] **`LogManager.CACHE_TTL_MILLIS` ist Instanzvariable**: `private final long CACHE_TTL_MILLIS = 60 * 1000;` (Z.40) → sollte `private static final` sein, da der Wert nicht pro Instanz variiert
- [x] **`OrderManager.CACHE_TTL_MILLIS` als lokale Variable**: Z.334 `long CACHE_TTL_MILLIS = 5 * 60 * 1000;` wird bei jedem `getOrders()`-Aufruf neu angelegt → als Klassenkonstante (`private static final`) auslagern (wie in `VendorManager`, `WarningManager` etc. korrekt gemacht)
- [ ] **`Tabs Font size`**: Tabs font size doesn't work in Settings panel. The UI doesn't change.
- [ ] **`Performance Fix`**: Fixes in all Manager classes and more stable Database usage.

---

## ✅ Tests

- [x] **`CategoryManagerText`** - Kein Test für `CategoryManager` vorhanden
- [x] **`OrderManagerTest`** – Kein Test für `OrderManager` vorhanden (kein `resetInstance()` erschwert Isolation)
- [x] **`LogManagerTest`** – Kein Test für `LogManager` vorhanden
- [x] **`SchedulerManagerTest`** – Kein Test für `SchedulerManager` vorhanden (Start/Stop, Intervall)
- [x] **`UpdateManagerTest`** – Kein Test für `UpdateManager` (Channel-Parsing, Version-Vergleich)
- [x] **`ImportUtilsTest`** – Kein Test für CSV-Import-Logik
- [x] **`ArticleExporterTest`** – Kein Test für Artikel-Export (CSV/PDF)
- [x] **`OrderExportTest`** – Kein Test für Bestellungs-PDF-Export
- [ ] **`QRCodeGeneratorTest`** – Kein Test für QR-Code-Generierung
- [ ] **`OrderLoggingUtilsTest`** – Kein Test für Bestell-Logging
- [ ] **`VendorOrderLoggingTest`** – Kein Test für Lieferanten-Bestell-Logging
- [ ] **`SettingsIntegrationTest`** – Integration-Test: leere Datei → Classpath-Fallback → save → reload mit Kommentaren

---

## 🔧 Technische Schulden (Tech Debt)

- [x] `settings.properties`-Ressource wird beim ersten Start nur geladen wenn die externe Datei **nicht** existiert oder leer ist – Migrationsschutz für bestehende User-Dateien ohne neue Schlüssel fehlt (automatisch fehlende Keys nachfüllen)
- [x] `DatabaseManager` – `resetInstance()` ist `public` und nur für Tests gedacht → mit `@VisibleForTesting` annotieren oder in `protected` umwandeln
- [x] `Main.settings` ist `public static` – Threading-Sicherheit durch `volatile` oder AtomicReference verbessern
- [x] `supplier_orders.txt` und `imported_qrcodes.txt` werden als einfache Text-Dateien geführt → in Datenbanktabellen überführen für Konsistenz
- [ ] Persistenz-Layer: JSON/YAML Backend hat keine Schema-Versionierung – Migrationsstrategie fehlt
- [x] `ArticleUtils.getCategoryForArticle()` liest `categories.json` über `Main.getAppDataDir()` – schwer testbar; in `CategoryManager` auslagern
- [x] **`ScanServer.STORE` und `QRCodeUtils.STORE`** definieren beide `new File(Main.getAppDataDir(), "scans.json")` → duplizierte Konstante; in einer gemeinsamen Klasse (z. B. `AppPaths`) zentralisieren
- [x] **`ScanServer` – Port 8080 hardkodiert** (`int port = 8080`) → über `settings.properties` konfigurierbar machen (neuer Key `scan_server_port`)
- [x] **`ScanServer` – unbegrenzter Thread-Pool**: `Executors.newCachedThreadPool()` kann unter Last Ressourcen erschöpfen → durch `newFixedThreadPool(n)` oder `newVirtualThreadPerTaskExecutor()` (Java 21) ersetzen
- [x] **`ScanServer.STORE` wird als `static final`-Feld bei Klassen-Laden initialisiert** → `Main.getAppDataDir()` muss zu diesem Zeitpunkt bereits initialisiert sein; Reihenfolge dokumentieren oder lazy initialization verwenden
- [x] **`@SuppressWarnings("DuplicatedCode")`** in `ArticleExporter`, `OrderExport`, `JFrameUtils` und mehreren GUIs deutet auf echten Code-Duplikat hin → gemeinsame Helfer-Methoden extrahieren statt Warnung unterdrücken
- [x] **`NotesManager.CACHE_TTL_MILLIS` als lokale Variable**: Z.177 `final long CACHE_TTL_MILLIS = 5 * 60 * 1000;` in `getAllNotes()` → ebenfalls als `private static final` auslagern
- [x] **Cache-TTL inkonsistent**: `LogManager` nutzt 1 Minute, alle anderen Manager nutzen 5 Minuten, `UpdateManager` 5 Minuten – keine gemeinsame Konstante; einen zentralen `CacheConfig`-Wert definieren oder zumindest dokumentieren warum die TTLs abweichen

---

## 📐 Code-Konsistenz

- [x] **`resetInstance()` fehlt** in `OrderManager`, `LogManager`, `NotesManager`, `WarningManager`, `UserManager`, `VendorManager`, `CategoryManager` → alle Singleton-Manager sollten für Tests resetbar sein (analog zu `ArticleManager`, `ClientManager`, `DatabaseManager`)
- [x] **`SimpleDateFormat` durch `DateTimeFormatter` ersetzen**: `NewOrderGUI` (2×), `MainGUI` (3×), `LogsGUI` (2×), `ArticleQrPreviewDialog` (1×) → einheitlich `LocalDate.now().format(DateTimeFormatter.ofPattern(...))` verwenden
- [x] **`OrderManager.extracted()`** – Methode trägt den von der IDE automatisch generierten Platzhalternamen → umbenennen in `serializeArticleEntry(Entry<String, Integer> e)`
- [ ] **`@SuppressWarnings("unused")`** auf Klassen-Ebene in `QRCodeUtils` und lokal in `MainGUI`/`ClientGUI` → ungenutzte Methoden/Felder entweder entfernen oder mit Kommentar dokumentieren, warum sie vorhanden sind
- [ ] **`UpdateManager` – `@SuppressWarnings("DoubleCheckedLocking")`**: `instance`-Feld ist `volatile`, die Unterdrückung ist technisch korrekt, wirkt aber wie ein Fehler → `@GuardedBy`-Kommentar ergänzen oder `enum`-Singleton-Muster verwenden
- [x] **Singleton-Muster vereinheitlichen**: `OrderManager` verwendet `getInstance()` mit lokalem `local`-Trick, andere Manager verwenden einfacheres DCL ohne lokale Variable → konsistentes Muster durch alle Manager

---

## ✨ Features / Verbesserungen

- [x] **Variables:** Klasse erstellen mit allen nutzvollen Variablen
- [x] **Einstellungen:** Fehlende Keys beim Programmstart automatisch mit Default-Werten aus dem Classpath-Template nachfüllen (statt nur `first-time` und `database_type`)
- [x] **Datenbank-Migration:** UI-Dialog zum Migrieren zwischen Backends (z. B. SQLite → H2) vollständig implementieren und testen
- [ ] ~~**Passwort-/Rollenkonzept:** *Benutzerverwaltung um Rollen (Admin, Benutzer, Gast) erweitern*~~
- [x] **Artikel-Verlauf:** Bestandsänderungen mit Zeitstempel/Benutzer protokollieren (Audit-Trail)
- [ ] **REST API:** Externe Integration über eine einfache HTTP-API (z. B. mit `ScanServer` erweitern)
- [ ] **Automatische Nachbestellungen:** Schwellenwert-basierte automatische Erstellung von Lieferanten-Bestellvorschlägen
- [ ] **Backup-Funktion:** Einzel-Klick-Backup der Datenbank und Einstellungen ins App-Datenverzeichnis
- [ ] **Export-Vorlagen:** Konfigurierbare PDF-Vorlagen für Bestellungen und Artikel-Listen
- [ ] **`ScanServer` – konfigurierbarer Port:** Neuen Key `scan_server_port` in `settings.properties` einlesen und beim Start nutzen
- [ ] **QR-Code-Import – Fehlerprotokoll:** Bei fehlgeschlagenen Imports (unbekannte Artikel-Nr.) detailliertere Fehlermeldung inkl. dem problematischen QR-Wert speichern/anzeigen
- [ ] **`Better Logging`**: Better Logging for Orders, Vendor Orders and Articles.

---

## 🎨 UI / UX

- [ ] **Spaltenbreiten-Einstellungen** in Tabellen persistent speichern (nach Neustart erhalten)
- [ ] **Tastaturkürzel** vollständig dokumentieren und in der Hilfe anzeigen (`KEYBOARD_SHORTCUTS.md` → In-App-Hilfe)
- [ ] **Leere Zustände:** Beim ersten Start oder bei leerer Datenbank einen „Willkommen"-Hinweis anzeigen statt leerer Tabellen
- [ ] **Undo/Redo:** Rückgängig-Funktion für kritische Aktionen (Löschen, Bestandsänderung)
- [ ] **Tabellenfilter** persistent speichern (zuletzt verwendeter Suchbegriff/Filter bleibt erhalten)
- [ ] **Artikelbilder:** Möglichkeit, einem Artikel ein Bild/Foto zuzuordnen
- [ ] **`ScanServer`-URL-Vorschau**: In den Einstellungen nach Eingabe der `server_url` einen „Test"-Button anbieten, der die URL auf Erreichbarkeit prüft

---

## 📦 Build & Deployment

- [ ] **CI/CD-Pipeline** (GitHub Actions) für automatischen Build + Test bei jedem Push einrichten
- [ ] **Installer:** nativen Installer mit `jpackage` (Windows `.msi`, macOS `.dmg`) erstellen
- [ ] **JavaDoc** vollständig generieren und alle `@param`/`@return`-Fehler beheben (aktuell 1 ERROR in Main.java)
- [ ] **Abhängigkeiten prüfen:** `ch.framedev.SimpleJavaUtils 1.2.4` – CVE-Check und ggf. aktualisieren
- [ ] **PDFBox 2.0.35** auf 3.x migrieren (2.x ist End-of-Life)

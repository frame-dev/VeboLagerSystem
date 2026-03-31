# 📦 VEBO Lagersystem

> **Ein modernes Lagerverwaltungssystem für VEBO Oensingen**

[![Version](https://img.shields.io/badge/version-0.3--TESTING-blue.svg)](https://github.com/frame-dev/VeboLagerSystem)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE)
[![Status](https://img.shields.io/badge/status-Active%20Development-green.svg)](https://github.com/frame-dev/VeboLagerSystem)

---

## 📋 Inhaltsverzeichnis

- [Überblick](#-überblick)
- [Hauptfunktionen](#-hauptfunktionen)
- [Screenshots](#-screenshots)
- [Technologie-Stack](#-technologie-stack)
- [Installation](#-installation)
- [Verwendung](#-verwendung)
- [Architektur](#-architektur)
- [Datenbank-Schema](#-datenbank-schema)
- [Funktionsdetails](#-funktionsdetails)
- [Konfiguration](#-konfiguration)
- [App-Datenverzeichnis](#-app-datenverzeichnis)
- [QR-Scan-Server (Optional)](#-qr-scan-server-optional)
- [Roadmap](#-roadmap)
- [Beiträge](#-beiträge)
- [Support](#-support)
- [Lizenz](#-lizenz)
- [Danksagungen](#-danksagungen)
- [Statistiken](#-statistiken)
- [Links](#-links)

---

## 🎯 Überblick

**VEBO Lagersystem** ist eine umfassende Desktop-Anwendung zur Verwaltung von Lagerbeständen, Artikeln, Bestellungen, Lieferanten und Abteilungen. Die Anwendung wurde speziell für die Bedürfnisse von VEBO Oensingen entwickelt und bietet eine moderne Benutzeroberfläche, eine robuste Datenhaltung und zahlreiche Automatisierungen.

### Hauptziele

- ✅ **Effiziente Lagerverwaltung:** Echtzeitübersicht über Bestände
- ✅ **Automatisierung:** Periodische Lagerchecks und Warnungen
- ✅ **Benutzerfreundlichkeit:** Moderne, konsistente UI mit Theme- und Font-Optionen
- ✅ **Nachvollziehbarkeit:** Protokolle, Audit-Trails und Logs
- ✅ **Flexibilität:** Anpassbare Einstellungen, QR-Workflow, Updates

---

## ✨ Hauptfunktionen

### 📦 Artikelverwaltung
- Vollständige Artikeldatenbank mit Artikelnummer, Details und Preisen
- Lagerbestandsverwaltung mit Min/Max-Levels
- Lieferantenzuordnung, Kategorien und Such-/Filterfunktionen
- **Bulk-Aktionen:** Mehrfach löschen, Bestand anpassen, Auswahl exportieren
- **QR-Code Features:**
  - QR-Codes für Artikel generieren
  - QR-Code Vorschau + PDF-Export
  - QR-Code Daten abrufen (Server/JSON)
- Detaillierte Lagerinformationen pro Artikel

### 🚚 Lieferantenverwaltung
- Lieferantendatenbank mit Kontaktdaten
- Artikel-Zuordnung und Suche
- Lieferanten-Logs und Lieferanten-Bestellungsprotokolle

### 📋 Bestellungsverwaltung
- Bestellung erstellen, bearbeiten und löschen
- Status-Tracking (Offen/Abgeschlossen)
- **Bestellung abschließen** (eigener Workflow)
- Filter (Offen/Abgeschlossen), Suche nach ID/Empfänger/Abteilung
- PDF-Export mit professionellem Layout
- Bestellprotokolle inkl. Benutzer-Tracking

### 🧾 Lieferanten-Bestellungen (Nachbestellungen)
- Eigene Oberfläche für Nachbestellungen
- Persistente Liste (`supplier_orders.txt`)
- Aktionen: Entfernen, Alle löschen, Speichern, Aktualisieren
- Protokollierung in `vendorOrder.log`

### 👥 Kunden & Abteilungen
- Kundenverwaltung inkl. Abteilungszuordnung
- Abteilungen mit Kontonummern (Departments)
- Import aus JSON-Ressourcen für Startdaten

### 📝 Notizen
- Persönliche Notizen mit Titel/Datum
- Erstellen, Bearbeiten, Löschen, Aktualisieren
- Split-View (Liste + Details) mit Such-/Filterkomfort

### ⚠️ Warnsystem
- Automatische Lagerbestandsprüfung
- Konfigurierbare Intervalle
- Warnungen bei Mindest- und kritischen Beständen
- Ungelesene Warnungen werden hervorgehoben

### 🔐 Benutzerverwaltung
- Benutzer-System mit Standard-User (Admin)
- Benutzer-Tracking in Bestellungen und Logs

### 📱 QR-Code-Integration
- QR-Scans aus Server-JSON importieren
- Automatische Import-Intervalle
- Duplikat-Tracking (`imported_qrcodes.txt`)
- Eigenverbrauch-Tracking (`own_use_list.txt`)
- Optionaler lokaler **QR-Scan-Server** (siehe unten)

### 📊 Logs & Protokolle
- Zentrales Logsystem mit DB-Logs und Datei-Logs
- Logs-UI mit:
  - Kategorien (Bestellungen, Lieferanten, Lieferantenbestellungen)
  - Such- und Datumsfilter
  - PDF/CSV-Export
  - Auto-Refresh

### 📤 Import/Export & Backup
- CSV-Import für Artikel, Lieferanten und Kunden
- CSV-Export aller Tabellen (Artikel, Lieferanten, Kunden, Bestellungen)
- Artikel-Export als PDF/CSV aus der Artikelverwaltung
- Startdaten-Import aus JSON-Ressourcen (inventar, vendor, clients, departments)

### 🎨 Theme- & UI-System
- Light/Dark Mode
- Anpassbare Accent/Header/Button-Farben
- Konfigurierbare Schriftarten
- Tabellen- und Tab-Fontgrößen
- Einheitliche UI-Elemente (Rounded Cards, Gradients)

### 🔄 Update-Check (GitHub Releases)
- Automatische Updateprüfung über GitHub Releases
- Release-Kanäle: Stable, Beta, Alpha, Testing
- Optionaler GitHub Token für höhere Rate-Limits

---

## 📸 Screenshots

### Hauptfenster (Tabbed Interface)
*Tabs für Artikel, Lieferanten, Bestellungen, Kunden, Einstellungen*

### Artikelverwaltung
*Bulk-Tools, QR-Buttons und Detailsicht*

### Bestellungen
*Bearbeiten, Abschließen, Filtern und Protokollieren*

### Lieferanten-Bestellungen
*Nachbestellungen verwalten und protokollieren*

### Notizen & Logs
*Notizverwaltung sowie Log-Analyse mit Export*

---

## 🛠️ Technologie-Stack

### Frontend
- **Java Swing** - Desktop-GUI
- **Custom UI Components** - Gradient Panels, Rounded Panels
- **Theme System** - Light/Dark + Custom Colors
- **HTML/CSS** - Formatierte Dialoge/Hilfetexte

### Backend
- **Java 21**
- **SQLite / H2** - Embedded Datenbank
- **Apache PDFBox 2.0.35** - PDF-Generierung
- **Log4j2** - Logging
- **Gson** - JSON-Verarbeitung
- **ZXing** - QR-Code-Generierung

### Build & Tools
- **Maven** - Build Management
- **SimpleJavaUtils** - Utilities
- **SnakeYAML** - YAML-Konfiguration
- **Commons Lang3** - Apache Commons Utilities

### Dependencies (Auszug)
```xml
<!-- Core -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.51.1.0</version>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.3.232</version>
</dependency>

<!-- PDF -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.35</version>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.24.3</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.24.3</version>
</dependency>

<!-- JSON -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>

<!-- QR-Code -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

---

## 📥 Installation

### Voraussetzungen

- **Java Development Kit (JDK) 21** oder höher
- **Maven 3.6+**
- **Git**

### Schritt-für-Schritt Installation

1. **Repository klonen**
```bash
git clone https://github.com/frame-dev/VeboLagerSystem.git
cd VeboLagerSystem
```

2. **Abhängigkeiten installieren**
```bash
mvn clean install
```

3. **Anwendung starten**
```bash
mvn exec:java -Dexec.mainClass="ch.framedev.lagersystem.main.Main"
```

### Alternative: JAR-Datei erstellen

```bash
mvn clean package
java -jar target/VeboLagerSystem-0.3-TESTING.jar
```

---

## 🚀 Verwendung

### Erster Start

- Datenbank und Einstellungen werden initialisiert
- Optionaler Import der Startdaten (Artikel/Lieferanten/Kunden/Abteilungen)
- Optionales QR-Code-Generieren für Startartikel
- Automatische Update-Prüfung (wenn Internet verfügbar)

### Artikel verwalten

- Artikel hinzufügen, bearbeiten, löschen
- Mehrfachaktionen: Bestand anpassen, Export, Bulk-Delete
- QR-Codes erzeugen, als PDF exportieren oder QR-Daten abrufen

### Bestellungen verwalten

- Neue Bestellung anlegen
- Bestehende Bestellungen bearbeiten
- Bestellungen abschließen (Statuswechsel)
- PDF-Export und Bestellprotokoll

### Lieferanten-Bestellungen

- Nachbestellungen sammeln und verwalten
- Einträge entfernen oder Liste speichern

### Notizen & Logs

- Notizen erstellen und bearbeiten
- Logs im UI filtern, suchen und exportieren

---

## 🏗️ Architektur

### Projekt-Struktur (Auszug)

```
VeboLagerSystem/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ch/framedev/lagersystem/
│   │   │       ├── actions/         # Action-Handler
│   │   │       │   └── OrderActions.java
│   │   │       ├── classes/         # Domain-Modelle
│   │   │       │   ├── Article.java
│   │   │       │   ├── Note.java
│   │   │       │   ├── Order.java
│   │   │       │   ├── User.java
│   │   │       │   ├── Vendor.java
│   │   │       │   └── Warning.java
│   │   │       ├── guis/            # GUI-Komponenten
│   │   │       │   ├── MainGUI.java
│   │   │       │   ├── ArticleGUI.java
│   │   │       │   ├── ArticleListGUI.java
│   │   │       │   ├── VendorGUI.java
│   │   │       │   ├── OrderGUI.java
│   │   │       │   ├── CompleteOrderGUI.java
│   │   │       │   ├── EditOrderGUI.java
│   │   │       │   ├── SupplierOrderGUI.java
│   │   │       │   ├── ClientGUI.java
│   │   │       │   ├── NotesGUI.java
│   │   │       │   ├── LogsGUI.java
│   │   │       │   ├── SettingsGUI.java
│   │   │       │   └── SplashscreenGUI.java
│   │   │       ├── main/            # Hauptklasse
│   │   │       │   └── Main.java
│   │   │       ├── managers/        # Business-Logic
│   │   │       │   ├── DatabaseManager.java
│   │   │       │   ├── ArticleManager.java
│   │   │       │   ├── VendorManager.java
│   │   │       │   ├── OrderManager.java
│   │   │       │   ├── ClientManager.java
│   │   │       │   ├── DepartmentManager.java
│   │   │       │   ├── NotesManager.java
│   │   │       │   ├── LogManager.java
│   │   │       │   ├── UpdateManager.java
│   │   │       │   ├── UserManager.java
│   │   │       │   ├── WarningManager.java
│   │   │       │   └── SchedulerManager.java
│   │   │       ├── scan/            # QR-Scan-Server
│   │   │       │   └── ScanServer.java
│   │   │       └── utils/           # Utilities
│   │   │           ├── ThemeManager.java
│   │   │           ├── LogUtils.java
│   │   │           ├── ImportUtils.java
│   │   │           ├── QRCodeUtils.java
│   │   │           ├── QRCodeGenerator.java
│   │   │           ├── OrderLoggingUtils.java
│   │   │           ├── VendorOrderLogging.java
│   │   │           └── UserDataDir.java
│   │   └── resources/              # Ressourcen
│   │       ├── logo.png
│   │       ├── logo-small.png
│   │       ├── settings.properties
│   │       ├── inventar.json
│   │       ├── vendor.json
│   │       ├── clients.json
│   │       ├── departments.json
│   │       └── categories.json
│   └── test/
├── pom.xml
└── README.md
```

### Design-Patterns

- **Singleton:** Manager-Klassen
- **Observer:** Theme-Updates für alle Fenster
- **Factory:** UI-Komponenten (Buttons)
- **MVC:** Trennung GUI / Business-Logic / Daten

---

## 🗃️ Datenbank-Schema

```sql
-- Articles
CREATE TABLE IF NOT EXISTS articles (
    articleNumber TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    details TEXT,
    stockQuantity INTEGER DEFAULT 0,
    minStockLevel INTEGER DEFAULT 0,
    sellPrice REAL DEFAULT 0.0,
    purchasePrice REAL DEFAULT 0.0,
    vendorName TEXT,
    category TEXT
);

-- Vendors
CREATE TABLE IF NOT EXISTS vendors (
    name TEXT PRIMARY KEY,
    contactPerson TEXT,
    phoneNumber TEXT,
    email TEXT,
    address TEXT
);

-- Orders
CREATE TABLE IF NOT EXISTS orders (
    orderId TEXT,
    orderedArticles TEXT,
    receiverName TEXT,
    receiverKontoNumber TEXT,
    orderDate TEXT,
    senderName TEXT,
    senderKontoNumber TEXT,
    department TEXT,
    status TEXT
);

-- Clients
CREATE TABLE IF NOT EXISTS clients (
    firstLastName TEXT PRIMARY KEY,
    department TEXT
);

-- Departments
CREATE TABLE IF NOT EXISTS departments (
    departmentName TEXT,
    kontoNumber TEXT
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    name TEXT PRIMARY KEY,
    created_at TEXT
);

-- Warnings
CREATE TABLE IF NOT EXISTS warnings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    articleNumber TEXT NOT NULL,
    message TEXT NOT NULL,
    warningType TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    isRead INTEGER DEFAULT 0
);

-- Notes
CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content VARCHAR(2555),
    date TEXT
);

-- Logs
CREATE TABLE IF NOT EXISTS logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT NOT NULL,
    level TEXT NOT NULL,
    message TEXT NOT NULL
);
```

---

## 🔧 Funktionsdetails

### Automatische Lagerbestandsprüfung

Der `SchedulerManager` führt regelmäßig Bestandsprüfungen durch:

- Intervall: 5–1440 Minuten (Standard: 30 Min.)
- Warnungen bei Unterschreitung des Mindestbestands
- Kritischer Bestand bei < 50% des Mindestbestands

### Warnungssystem

- `LOW_STOCK`: Bestand unter Mindestbestand
- `CRITICAL_STOCK`: Bestand < 50% des Mindestbestands
- Anzeige in Popups, nur ungelesene Warnungen

### QR-Code-Workflow

- QR-Codes für Artikel erstellen und als PNG speichern
- QR-Preview + PDF-Export aus der Artikelverwaltung
- Scans als JSON importieren (Server-URL konfigurierbar)
- Eigenverbrauchs-Tracking (optional)

### Update-Check

- Prüfung über GitHub Releases
- Unterstützt Stable/Beta/Alpha/Testing
- GitHub Token optional (höhere Rate-Limits)

---

## ⚙️ Konfiguration

Die Einstellungen werden in `settings.properties` im App-Datenverzeichnis gespeichert.

Beispiel (wird automatisch gepflegt):

```properties
# First run / Import
first-time=true
load-from-files=true

# Scheduler
stock_check_interval=30
enable_hourly_warnings=true
warning_display_interval=1
enable_auto_stock_check=true

# Database
# Supported values: sqlite, h2, json, yaml
database_type=h2
database_file=

# QR-Code Import
enable_automatic_import_qrcode=true
qrcode_import_interval=10
server_url=https://framedev.ch/vebo/scans.json

# Theme / UI
dark_mode=false
table_font_size=16
table_font_size_tab=15
font_style=Dialog
theme_accent_color=
theme_header_color=
theme_button_color=

# Update-Manager
github-token=
```

---

## 💾 App-Datenverzeichnis

**Speicherort:**
- Windows: `%APPDATA%/VeboLagerSystem/`
- macOS: `~/Library/Application Support/VeboLagerSystem/`
- Linux: `$XDG_DATA_HOME/VeboLagerSystem/` oder `~/.local/share/VeboLagerSystem/`

**Inhalt (Auszug):**
```
VeboLagerSystem/
├── vebo_lager_system.db      # SQLite-Datenbank
├── vebo_lager_system.mv.db   # H2-Datenbank (bei database_type=h2)
├── vebo_lager_system_json/   # JSON-Dateispeicher (bei database_type=json)
│   └── tables/
├── vebo_lager_system_yaml/   # YAML-Dateispeicher (bei database_type=yaml)
│   └── tables/
├── settings.properties       # Einstellungen
├── scans.json                # QR-Scan-Daten
├── qr_codes/                 # Generierte QR-Codes
├── supplier_orders.txt       # Lieferanten-Nachbestellungen
├── imported_qrcodes.txt      # QR-Code-Import-Tracking
├── imported_items.txt        # Import-Tracking
├── own_use_list.txt          # Eigenverbrauch-Tracking
└── logs/
    ├── vebo_lager_system.log # App-Log
    ├── bestellung.log        # Bestell-Log
    └── vendorOrder.log       # Lieferanten-Log
```

---

## 📡 QR-Scan-Server (Optional)

Ein kleiner HTTP-Server für QR-Scans ist im Projekt enthalten (`ScanServer`).

**Start:**
```bash
mvn exec:java -Dexec.mainClass="ch.framedev.lagersystem.scan.ScanServer"
```

**Endpoints:**
- `/scan?data=...` (Formular + Speicherung)
- `/list` (JSON aller Scans)
- `/latest` (letzter Scan als JSON)

---

## 🗺️ Roadmap

### Version 0.4 (Geplant)
- [ ] REST API für externe Integration
- [ ] Mobile App (iOS/Android)
- [ ] Cloud-Synchronisation
- [ ] Erweiterte QR-Code-Funktionen
- [ ] Barcode-Scanner-Integration

### Version 0.5 (Geplant)
- [ ] Multi-Mandantenfähigkeit
- [ ] Erweiterte Berechtigungen
- [ ] Workflows und Genehmigungsprozesse
- [ ] Integration mit ERP-Systemen
- [ ] Erweiterte Analytics

### Langfristige Ziele
- [ ] KI-basierte Bestandsprognosen
- [ ] Automatische Nachbestellungen
- [ ] Lieferanten-Portal
- [ ] Kunden-Portal
- [ ] IoT-Integration für automatisches Tracking

---

## 🤝 Beiträge

1. Fork das Repository
2. Feature Branch erstellen (`git checkout -b feature/AmazingFeature`)
3. Commit (`git commit -m 'Add some AmazingFeature'`)
4. Push (`git push origin feature/AmazingFeature`)
5. Pull Request öffnen

---

## 📞 Support

- **Entwickler:** Darryl Huber
- **Organisation:** VEBO Oensingen
- **E-Mail:** [support@vebo.ch](mailto:support@vebo.ch)
- **Website:** [https://vebo.ch](https://vebo.ch)

### Probleme melden

1. Issue erstellen (GitHub)
2. Beschreibung des Problems
3. Schritte zur Reproduktion
4. Relevante Log-Dateien (falls vorhanden)

### FAQ

**Q: Wie ändere ich das Theme?**  
A: Einstellungen → Darstellung → Theme/Colors anpassen

**Q: Wo werden die Daten gespeichert?**  
A: Im App-Datenverzeichnis unter `VeboLagerSystem/` (siehe Abschnitt App-Datenverzeichnis)

**Q: Kann ich Daten aus Excel importieren?**  
A: Ja, über CSV-Export aus Excel und Import in VEBO Lagersystem

**Q: Funktioniert das System offline?**  
A: Ja, nur der QR-Code-Import und Update-Check benötigen eine Server-Verbindung

**Q: Gibt es einen QR-Scan-Server?**  
A: Ja, optional über `ScanServer` (siehe Abschnitt QR-Scan-Server)

---

## 📄 Lizenz

**Proprietary License**

© 2026 VEBO Oensingen. Alle Rechte vorbehalten.

Diese Software ist Eigentum von VEBO Oensingen und darf ohne ausdrückliche schriftliche Genehmigung nicht kopiert, verändert oder verteilt werden.

---

## 🙏 Danksagungen

- **VEBO Oensingen** für Anforderungen und Feedback
- **Apache Software Foundation** für PDFBox und Log4j
- **SQLite Team** für die embedded Datenbank
- **ZXing Project** für QR-Code-Generierung
- **Java Community** für Libraries und Tools

---

## 📊 Statistiken

- **Version:** 0.3-TESTING
- **Stand:** Februar 2026
- **Java Dateien:** 46
- **Zeilen Code (Java):** ~27k
- **Dependencies:** 10
- **Supported Platforms:** Windows, macOS, Linux

---

## 🔗 Links

- [GitHub Repository](https://github.com/frame-dev/VeboLagerSystem)
- [VEBO Website](https://vebo.ch)
- [Issue Tracker](https://github.com/frame-dev/VeboLagerSystem/issues)
- [Wiki/Documentation](https://github.com/frame-dev/VeboLagerSystem/wiki)

---

<div align="center">

**Made with ❤️ by Darryl Huber for VEBO Oensingen**

[⬆ Back to Top](#-vebo-lagersystem)

</div>

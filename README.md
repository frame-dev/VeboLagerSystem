# 📦 VEBO Lagersystem

> **Ein modernes Lagerverwaltungssystem für VEBO Oensingen**

[![Version](https://img.shields.io/badge/version-0.2--TESTING-blue.svg)](https://github.com/frame-dev/VeboLagerSystem)
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
- [Funktionsdetails](#-funktionsdetails)
- [Entwicklung](#-entwicklung)
- [Konfiguration](#-konfiguration)
- [Roadmap](#-roadmap)
- [Beiträge](#-beiträge)
- [Support](#-support)
- [Lizenz](#-lizenz)

---

## 🎯 Überblick

**VEBO Lagersystem** ist eine umfassende Desktop-Anwendung zur Verwaltung von Lagerbeständen, Artikeln, Bestellungen und Lieferanten. Die Anwendung wurde speziell für die Bedürfnisse von VEBO Oensingen entwickelt und bietet eine intuitive, moderne Benutzeroberfläche mit umfangreichen Funktionen.

### Hauptziele

- ✅ **Effiziente Lagerverwaltung:** Echtzeitübersicht über alle Lagerbestände
- ✅ **Automatisierung:** Automatische Bestandsprüfung und Warnmeldungen
- ✅ **Benutzerfreundlichkeit:** Moderne, intuitive Benutzeroberfläche
- ✅ **Nachvollziehbarkeit:** Vollständige Audit-Trails und Logging
- ✅ **Flexibilität:** Anpassbare Einstellungen und Themes

---

## ✨ Hauptfunktionen

### 📦 Artikelverwaltung
- **Vollständige Artikeldatenbank** mit Artikelnummer, Name, Details
- **Lagerbestandsverwaltung** mit Min/Max-Levels
- **Preisverwaltung** (Einkaufs- und Verkaufspreise)
- **Lieferantenzuordnung** für jeden Artikel
- **Kategorisierung** und Filterung
- **Suchfunktion** für schnellen Zugriff
- **Import/Export** von Artikeldaten (CSV)
- **QR-Code-Unterstützung** für Artikelidentifikation

### 🚚 Lieferantenverwaltung
- **Lieferantendatenbank** mit vollständigen Kontaktdaten
- **Verwaltung von:**
  - Firmenname und Kontaktperson
  - Telefonnummer und E-Mail
  - Adresse
  - Zugeordnete Artikel
- **Import/Export** von Lieferantendaten
- **Schnellsuche** und Filterung

### 📋 Bestellungsverwaltung
- **Bestellungserstellung** mit mehreren Artikeln
- **Empfänger- und Absenderverwaltung**
- **Abteilungszuordnung**
- **Automatische Preisberechnung**
- **Bestellstatus-Tracking** (Ausstehend, Abgeschlossen, Storniert)
- **PDF-Export** für Bestellungen mit professionellem Layout
- **Bestellhistorie** mit vollständiger Nachverfolgung
- **Bestellprotokollierung** mit Zeitstempel und Benutzer

### 👥 Kundenverwaltung
- **Kundendatenbank** mit Namen und Abteilungen
- **Abteilungsverwaltung**
- **Schnelle Kundensuche**
- **Import/Export** von Kundendaten

### ⚠️ Warnsystem
- **Automatische Lagerbestandsprüfung**
  - Konfigurierbare Prüfintervalle (5-1440 Minuten)
  - Warnungen bei Unterschreitung des Mindestbestands
- **Warnungsanzeige**
  - Automatische Popup-Benachrichtigungen
  - Konfigurierbare Anzeigeintervalle (1-24 Stunden)
  - Ungelesene Warnungen werden hervorgehoben
- **Warnungstypen:**
  - Niedriger Lagerbestand
  - Kritischer Lagerbestand (< 50% des Mindestbestands)
- **Warnungshistorie** für Nachverfolgung

### 📱 QR-Code-Integration
- **Automatischer QR-Code-Import** von Server
- **Konfigurierbare Import-Intervalle** (1-60 Minuten)
- **Artikelzuordnung** über QR-Codes
- **Scan-Historie** mit Tracking
- **Server-Synchronisation** für zentrale QR-Code-Verwaltung

### 🎨 Theme-System
- **Light Mode:** Heller, professioneller Look
- **Dark Mode:** Augenschonend für Nachtarbeit
- **Automatische Theme-Anwendung** auf alle Komponenten
- **Konsistente Farbgebung:**
  - Primary Blue: Professional Blue für Header
  - Accent Colors: Für Buttons und Highlights
  - Success/Warning/Error Colors: Für Status-Feedback
- **Theme-Persistenz:** Einstellungen bleiben erhalten

### ⚙️ Einstellungen & Konfiguration
- **System-Einstellungen:**
  - Lagerbestandsprüfung konfigurieren
  - Warnungsanzeige anpassen
  - QR-Code-Import einstellen
  - Font-Größe für Tabellen
  - Theme-Auswahl (Light/Dark)
- **Verbindungseinstellungen:**
  - Server-URL für QR-Code-Scans
- **Datenbankmanagement:**
  - Datenbank bereinigen
  - Einzelne Tabellen löschen
  - Sicherheitsabfragen
- **Import/Export:**
  - CSV-Import für Artikel, Lieferanten, Kunden
  - CSV-Export aller Tabellen
  - Bestellungen als PDF exportieren

### 📊 Reporting & Export
- **PDF-Export:**
  - Bestellungen mit vollständigen Details
  - Professionelles Layout mit Logo
  - Preisaufstellungen
- **CSV-Export:**
  - Artikel, Lieferanten, Kunden, Bestellungen
  - Vollständige Datenexporte
  - Backup-Funktionalität
- **Logs & Audit-Trails:**
  - Anwendungs-Log (application.log)
  - Bestellungs-Log (bestellung.log)
  - Nachverfolgung aller Änderungen

### 🔐 Benutzerverwaltung
- **User-System** mit Namen
- **Standard-User:** Admin wird automatisch erstellt
- **Benutzer-Tracking** bei Bestellungen
- **Audit-Trail** für alle Aktionen

### 💾 Datenbankmanagement
- **SQLite-Datenbank** für lokale Speicherung
- **Automatische Tabellenerstellung**
- **Datenintegrität** durch Constraints
- **Backup-Funktionen** durch Export
- **Datenbank-Bereinigung** für Fresh-Start

---

## 📸 Screenshots

### Hauptfenster (Tabbed Interface)
*Moderne Benutzeroberfläche mit Tabs für alle Hauptfunktionen*

### Artikelverwaltung
*Vollständige Übersicht mit Such- und Filterfunktionen*

### Neue Bestellung
*Intuitive Bestellerfassung mit Artikelauswahl und Preisberechnung*

### Einstellungen
*Umfassende Konfigurationsmöglichkeiten mit Live-Preview*

### Dark Mode
*Augenschonendes Design für Nachtarbeit*

---

## 🛠️ Technologie-Stack

### Frontend
- **Java Swing** - Moderne Desktop-GUI
- **Custom UI Components** - Gradient Panels, Rounded Panels
- **Theme System** - Light/Dark Mode Support
- **HTML/CSS** - Für formatierte Hilfe-Dialoge

### Backend
- **Java 21** - Moderne Java-Features
- **SQLite** - Embedded Datenbank
- **Apache PDFBox** - PDF-Generierung
- **Apache Log4j2** - Professionelles Logging
- **Gson** - JSON-Verarbeitung

### Build & Tools
- **Maven** - Build Management
- **SimpleJavaUtils** - Utility-Bibliothek
- **SnakeYAML** - YAML-Konfiguration
- **Commons Lang3** - Apache Commons Utilities

### Dependencies
```xml
<!-- Core -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.51.1.0</version>
</dependency>

<!-- PDF -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.24.3</version>
</dependency>

<!-- JSON -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
```

---

## 📥 Installation

### Voraussetzungen

- **Java Development Kit (JDK) 21** oder höher
- **Maven 3.6+** für Build
- **Git** für Repository-Zugriff

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

3. **Anwendung kompilieren**
```bash
mvn compile
```

4. **Anwendung starten**
```bash
mvn exec:java -Dexec.mainClass="ch.framedev.lagersystem.main.Main"
```

### Alternative: JAR-Datei erstellen

```bash
mvn clean package
java -jar target/VeboLagerSystem-0.2-TESTING.jar
```

---

## 🚀 Verwendung

### Erster Start

1. **Automatische Initialisierung:**
   - Datenbank wird erstellt
   - Standard-User "Admin" wird angelegt
   - Beispieldaten werden importiert (optional)

2. **Hauptfenster öffnet sich:**
   - Tab "Artikel" für Artikelverwaltung
   - Tab "Lieferanten" für Lieferantenverwaltung
   - Tab "Bestellungen" für Bestellungsverwaltung
   - Tab "Kunden" für Kundenverwaltung

### Artikel verwalten

1. **Neuen Artikel hinzufügen:**
   - Button "➕ Neuer Artikel" klicken
   - Formular ausfüllen (Artikelnummer, Name, Details, etc.)
   - Speichern

2. **Artikel bearbeiten:**
   - Artikel in Tabelle auswählen
   - Button "✏️ Bearbeiten" klicken
   - Änderungen vornehmen
   - Speichern

3. **Artikel löschen:**
   - Artikel auswählen
   - Button "🗑️ Löschen" klicken
   - Bestätigen

### Bestellung erstellen

1. **Neue Bestellung:**
   - Tab "Bestellungen" → "Neue Bestellung"
   - Empfänger und Absender auswählen
   - Abteilung zuordnen
   - Artikel hinzufügen mit Mengen
   - Button "📦 Bestellen" klicken

2. **PDF exportieren:**
   - Button "📄 Export PDF" klicken
   - PDF wird im Benutzerverzeichnis gespeichert

### Einstellungen anpassen

1. **Einstellungen öffnen:**
   - Button "⚙️ Einstellungen" im Header

2. **Tabs durchgehen:**
   - **System:** Automatisierung konfigurieren
   - **Verbindung:** Server-URL einstellen
   - **Datenbank:** Datenbank verwalten
   - **Import/Export:** Daten sichern/wiederherstellen
   - **Über:** Versionsinformationen

---

## 🏗️ Architektur

### Projekt-Struktur

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
│   │   │       │   ├── Order.java
│   │   │       │   ├── User.java
│   │   │       │   ├── Vendor.java
│   │   │       │   └── Warning.java
│   │   │       ├── guis/            # GUI-Komponenten
│   │   │       │   ├── MainGUI.java
│   │   │       │   ├── ArticleGUI.java
│   │   │       │   ├── VendorGUI.java
│   │   │       │   ├── OrderGUI.java
│   │   │       │   ├── ClientGUI.java
│   │   │       │   ├── NewOrderGUI.java
│   │   │       │   └── SettingsGUI.java
│   │   │       ├── main/            # Hauptklasse
│   │   │       │   └── Main.java
│   │   │       ├── managers/        # Business-Logic
│   │   │       │   ├── DatabaseManager.java
│   │   │       │   ├── ArticleManager.java
│   │   │       │   ├── VendorManager.java
│   │   │       │   ├── OrderManager.java
│   │   │       │   ├── ClientManager.java
│   │   │       │   ├── UserManager.java
│   │   │       │   ├── WarningManager.java
│   │   │       │   └── SchedulerManager.java
│   │   │       ├── scan/            # QR-Code Scanning
│   │   │       │   └── ScanImporter.java
│   │   │       └── utils/           # Utilities
│   │   │           ├── ThemeManager.java
│   │   │           ├── LogUtils.java
│   │   │           ├── ImportUtils.java
│   │   │           ├── OrderLoggingUtils.java
│   │   │           └── UserDataDir.java
│   │   └── resources/              # Ressourcen
│   │       ├── logo.png
│   │       ├── logo-small.png
│   │       ├── settings.properties
│   │       ├── inventar.json
│   │       ├── vendor.json
│   │       ├── clients.json
│   │       └── categories.json
│   └── test/                       # Tests
│       └── java/
├── logs/                           # Log-Dateien
│   ├── application.log
│   └── bestellung.log
├── Docs/                           # Dokumentation
├── pom.xml                         # Maven-Konfiguration
└── README.md                       # Diese Datei
```

### Design-Patterns

- **Singleton Pattern:** Manager-Klassen (ArticleManager, ThemeManager, etc.)
- **Observer Pattern:** Theme-Updates für alle Fenster
- **Factory Pattern:** Button-Erstellung mit Theme-Support
- **MVC Pattern:** Trennung von GUI, Business-Logic und Daten

### Datenbank-Schema

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
    orderId TEXT PRIMARY KEY,
    receiverName TEXT NOT NULL,
    receiverKontoNumber TEXT NOT NULL,
    senderName TEXT NOT NULL,
    senderKontoNumber TEXT NOT NULL,
    orderedArticles TEXT NOT NULL,
    orderDate TEXT NOT NULL,
    status TEXT DEFAULT 'Ausstehend',
    department TEXT
);

-- Clients
CREATE TABLE IF NOT EXISTS clients (
    firstLastName TEXT PRIMARY KEY,
    department TEXT
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
```

---

## 🔧 Funktionsdetails

### Automatische Lagerbestandsprüfung

Der `SchedulerManager` führt automatisch Bestandsprüfungen durch:

```java
// Konfigurierbar über Einstellungen
- Prüfintervall: 5-1440 Minuten (Standard: 30 Min.)
- Prüft alle Artikel auf Mindestbestand
- Erstellt Warnungen bei Unterschreitung
- Unterscheidet zwischen:
  * Niedriger Bestand (< Mindestbestand)
  * Kritischer Bestand (< 50% des Mindestbestands)
```

### Warnungssystem

**Warnungstypen:**
- `LOW_STOCK`: Lagerbestand unter Mindestbestand
- `CRITICAL_STOCK`: Lagerbestand unter 50% des Mindestbestands

**Automatische Anzeige:**
- Konfigurierbar: 1-24 Stunden
- Nur ungelesene Warnungen werden angezeigt
- Popup-Dialog mit Details
- Warnungen können als gelesen markiert werden

### QR-Code-Integration

**Server-basiertes System:**
```
1. QR-Codes werden über Web-Interface gescannt
2. Server speichert Scan-Daten in Datenbank
3. VeboLagerSystem importiert automatisch:
   - list.php liefert JSON-Array mit Scans
   - Artikel werden zugeordnet und verarbeitet
   - Tracking verhindert Duplikate
```

**Import-Prozess:**
```java
// Automatischer Import alle X Minuten
1. GET Request an Server-URL
2. JSON-Parsing der Scan-Daten
3. Artikelzuordnung über Nummer
4. Verarbeitung und Logging
5. Tracking in imported_qrcodes.txt
```

### PDF-Export für Bestellungen

**Features:**
- Professionelles Layout mit Logo
- Vollständige Bestelldetails:
  - Empfänger und Absender
  - Datum und Bestellnummer
  - Artikelliste mit Preisen
  - Gesamtpreis
- Automatische Seitenumbrüche
- Unicode-Support (UTF-8)
- Speicherung im Benutzerverzeichnis

### Theme-System

**Light Mode Colors:**
```java
Background: #F5F7FA
Card: #FFFFFF
Primary: #2980B9 (Blue)
Text: #1F2D3D (Dark Gray)
```

**Dark Mode Colors:**
```java
Background: #1A1A1A
Card: #2D2D2D
Primary: #1E3A5F (Dark Blue)
Text: #F0F0F0 (Light Gray)
```

**Theme-Anwendung:**
- Automatisch auf alle Komponenten
- Persistente Speicherung
- Live-Update ohne Neustart
- UIManager-Integration für System-Dialoge

---

## 👨‍💻 Entwicklung

### Development Setup

```bash
# Repository klonen
git clone https://github.com/frame-dev/VeboLagerSystem.git
cd VeboLagerSystem

# Dependencies installieren
mvn clean install

# Kompilieren
mvn compile

# Tests ausführen
mvn test

# Anwendung starten (Development)
mvn exec:java
```

### Code-Style

- **Java Code Conventions** von Oracle
- **Kommentare auf Deutsch** für Business-Logic
- **JavaDoc** für öffentliche APIs
- **Logging:** Log4j2 für alle wichtigen Ereignisse

### Testing

```bash
# Alle Tests ausführen
mvn test

# Spezifischen Test ausführen
mvn test -Dtest=ArticleManagerTest

# Test Coverage
mvn jacoco:report
```

### Debugging

**Log-Dateien:**
- `logs/application.log` - Haupt-Anwendungslog
- `logs/bestellung.log` - Bestellungs-spezifisches Log

**Log-Level:**
```properties
# In log4j2.xml konfigurierbar
DEBUG - Detaillierte Debug-Informationen
INFO - Normale Informationen
WARN - Warnungen
ERROR - Fehler
```

### Build

```bash
# Clean Build
mvn clean package

# Skip Tests
mvn clean package -DskipTests

# Mit Dependencies
mvn clean package -DincludeDependencies
```

---

## ⚙️ Konfiguration

### Settings.properties

Die Anwendung speichert Einstellungen in `settings.properties`:

```properties
# Automatisierung
stock_check_interval=30
enable_auto_stock_check=true
enable_hourly_warnings=true
warning_display_interval=1

# QR-Code Import
enable_automatic_import_qrcode=false
qrcode_import_interval=10
server_url=http://localhost/scan/list.php

# Theme
dark_mode=false
table_font_size=16

# Application
last_user=Admin
```

### Datenverzeichnis

**Speicherort:**
- Windows: `%APPDATA%/VeboLagerSystem/`
- macOS: `~/Library/Application Support/VeboLagerSystem/`
- Linux: `~/.VeboLagerSystem/`

**Inhalt:**
```
VeboLagerSystem/
├── database.db              # SQLite-Datenbank
├── settings.properties      # Einstellungen
├── own_use_list.txt        # Eigenverbrauch-Tracking
├── imported_qrcodes.txt    # QR-Code-Import-Tracking
├── imported_items.txt      # Artikel-Import-Tracking
└── logs/                   # Log-Dateien
    ├── application.log
    └── bestellung.log
```

---

## 🗺️ Roadmap

### Version 0.3 (Geplant)
- [ ] Erweiterte Suchfunktionen mit Filtern
- [ ] Benutzerrechteverwaltung mit Rollen
- [ ] Dashboard mit Statistiken
- [ ] Erweiterte Reporting-Funktionen
- [ ] Multi-Sprach-Unterstützung (DE/EN/FR)

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

### Contribution Guidelines

1. **Fork** das Repository
2. Erstelle einen **Feature Branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit** deine Änderungen (`git commit -m 'Add some AmazingFeature'`)
4. **Push** zum Branch (`git push origin feature/AmazingFeature`)
5. Öffne einen **Pull Request**

### Code Review Process

- Alle Pull Requests werden reviewed
- Mindestens 1 Approval erforderlich
- Alle Tests müssen passing sein
- Code Coverage sollte nicht sinken

### Coding Standards

- Java Code Conventions befolgen
- JavaDoc für öffentliche APIs
- Unit Tests für neue Features
- Logging für wichtige Operationen

---

## 📞 Support

### Kontakt

- **Entwickler:** Darryl Huber
- **Organisation:** VEBO Oensingen
- **E-Mail:** [support@vebo.ch](mailto:support@vebo.ch)
- **Website:** [https://vebo.ch](https://vebo.ch)

### Probleme melden

Bei Problemen oder Fragen:

1. **Issue erstellen** auf GitHub
2. **Beschreibung** des Problems
3. **Screenshots** wenn möglich
4. **Log-Dateien** anhängen
5. **Schritte zur Reproduktion** angeben

### FAQ

**Q: Wie ändere ich das Theme?**
A: Einstellungen → System → Design & Darstellung → Dark Mode aktivieren

**Q: Wo werden die Daten gespeichert?**
A: Im Benutzerverzeichnis unter `VeboLagerSystem/` (siehe Konfiguration)

**Q: Kann ich Daten aus Excel importieren?**
A: Ja, über CSV-Export aus Excel und Import in VeboLagerSystem

**Q: Funktioniert das System offline?**
A: Ja, nur der QR-Code-Import benötigt eine Server-Verbindung

**Q: Kann ich die Datenbank sichern?**
A: Ja, über Einstellungen → Import/Export → Datenbank Exportieren

---

## 📄 Lizenz

**Proprietary License**

© 2026 VEBO Oensingen. Alle Rechte vorbehalten.

Diese Software ist Eigentum von VEBO Oensingen und darf ohne ausdrückliche schriftliche Genehmigung nicht kopiert, verändert oder verteilt werden.

**Disclaimer:**
Diese Software wird "wie sie ist" bereitgestellt, ohne jegliche Garantie. Der Autor haftet nicht für Schäden, die durch die Verwendung dieser Software entstehen.

---

## 🙏 Danksagungen

- **VEBO Oensingen** für die Unterstützung und Anforderungen
- **Apache Software Foundation** für PDFBox und Log4j
- **SQLite Team** für die embedded Datenbank
- **Java Community** für Libraries und Tools

---

## 📊 Statistiken

- **Version:** 0.2-TESTING
- **Release Date:** Januar 2026
- **Lines of Code:** ~12,000+
- **Classes:** 50+
- **Dependencies:** 15+
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

<!doctype html>
<html lang="de">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Hilfe - Scanner System</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(20px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        @keyframes slideDown {
            from {
                opacity: 0;
                transform: translateY(-30px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
            position: relative;
            overflow-x: hidden;
        }

        body::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
                        radial-gradient(circle at 80% 70%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
            pointer-events: none;
        }

        .container {
            max-width: 900px;
            margin: 0 auto;
            position: relative;
            z-index: 1;
        }

        .vebo-logo {
            max-width: 400px;
            height: auto;
            margin: auto;
            display: block;
            animation: fadeInDown 0.6s ease-out;
            filter: drop-shadow(2px 2px 4px rgba(0, 0, 0, 0.2));
        }

        h1 {
            color: white;
            margin-bottom: 30px;
            font-size: 32px;
            text-align: center;
            text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
            animation: slideDown 0.6s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .help-card {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 30px;
            border-radius: 16px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            margin-bottom: 25px;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) backwards;
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }

        .help-card:nth-child(2) { animation-delay: 0.1s; }
        .help-card:nth-child(3) { animation-delay: 0.2s; }
        .help-card:nth-child(4) { animation-delay: 0.3s; }
        .help-card:nth-child(5) { animation-delay: 0.4s; }
        .help-card:nth-child(6) { animation-delay: 0.5s; }

        .help-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 12px 48px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
        }

        .help-title {
            font-size: 24px;
            font-weight: 600;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            margin-bottom: 15px;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .help-icon {
            font-size: 28px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            width: 50px;
            height: 50px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        }

        .help-content {
            color: #333;
            line-height: 1.8;
            font-size: 15px;
        }

        .help-content p {
            margin-bottom: 12px;
        }

        .help-content ul {
            margin-left: 20px;
            margin-bottom: 12px;
        }

        .help-content li {
            margin-bottom: 8px;
            color: #555;
        }

        .help-content strong {
            color: #667eea;
            font-weight: 600;
        }

        .code-example {
            background: #f5f7ff;
            border-left: 4px solid #667eea;
            padding: 12px 15px;
            border-radius: 8px;
            font-family: 'Courier New', monospace;
            font-size: 14px;
            color: #333;
            margin: 12px 0;
        }

        .tip-box {
            background: #e8f5e9;
            border-left: 4px solid #4caf50;
            padding: 15px;
            border-radius: 8px;
            margin: 15px 0;
        }

        .tip-box strong {
            color: #4caf50;
        }

        .warning-box {
            background: #fff3e0;
            border-left: 4px solid #ff9800;
            padding: 15px;
            border-radius: 8px;
            margin: 15px 0;
        }

        .warning-box strong {
            color: #ff9800;
        }

        .back-link {
            display: inline-block;
            margin: 20px auto;
            padding: 14px 35px;
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            color: #667eea;
            text-decoration: none;
            border-radius: 10px;
            font-weight: 600;
            text-align: center;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
            position: relative;
            overflow: hidden;
        }

        .back-link::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.2), transparent);
            transition: left 0.5s;
        }

        .back-link:hover::before {
            left: 100%;
        }

        .back-link:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 30px rgba(0, 0, 0, 0.25);
            background: rgba(255, 255, 255, 1);
        }

        .links {
            text-align: center;
            margin-bottom: 30px;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.6s backwards;
        }

        @media (max-width: 768px) {
            h1 {
                font-size: 28px;
            }

            .help-card {
                padding: 20px;
            }

            .help-title {
                font-size: 20px;
            }
        }
    </style>
</head>

<body>
    <div class="container">
        <img src="logo.png" alt="VEBO Logo" class="vebo-logo">
        
        <h1>📚 Hilfe & Anleitung</h1>

        <div class="help-card">
            <div class="help-title">
                <div class="help-icon">📷</div>
                <span>QR-Code & Barcode scannen</span>
            </div>
            <div class="help-content">
                <p>Das System unterstützt das Scannen von QR-Codes und Barcodes über die Kamera Ihres Geräts.</p>
                
                <p><strong>So scannen Sie einen Code:</strong></p>
                <ul>
                    <li>Klicken Sie auf "Neuer Scan" auf der Startseite</li>
                    <li>Drücken Sie den Button "Kamera starten"</li>
                    <li>Halten Sie den QR-Code oder Barcode vor die Kamera</li>
                    <li>Das System erkennt automatisch den Code und extrahiert die Daten</li>
                </ul>

                <div class="tip-box">
                    <strong>💡 Tipp:</strong> Für beste Ergebnisse halten Sie den Code ruhig und achten Sie auf gute Beleuchtung.
                </div>
            </div>
        </div>

        <div class="help-card">
            <div class="help-title">
                <div class="help-icon">📝</div>
                <span>Datenformat verstehen</span>
            </div>
            <div class="help-content">
                <p>Das System kann strukturierte Daten aus QR-Codes automatisch parsen und anzeigen.</p>
                
                <p><strong>Unterstütztes Format:</strong></p>
                <div class="code-example">
                    artikelNr:001,name:Produktname,buyPrice:25.50;sellPrice:28.80;vendor:Lieferant
                </div>

                <p><strong>Erkannte Felder:</strong></p>
                <ul>
                    <li><strong>artikelNr:</strong> Artikel-Nummer</li>
                    <li><strong>name:</strong> Produktname</li>
                    <li><strong>buyPrice:</strong> Einkaufspreis (in CHF)</li>
                    <li><strong>sellPrice:</strong> Verkaufspreis (in CHF)</li>
                    <li><strong>vendor:</strong> Lieferant/Hersteller</li>
                </ul>

                <p>Felder können durch Komma (,) oder Semikolon (;) getrennt werden.</p>
            </div>
        </div>

        <div class="help-card">
            <div class="help-title">
                <div class="help-icon">📊</div>
                <span>Scan-Typen</span>
            </div>
            <div class="help-content">
                <p>Bei jedem Scan können Sie zwischen zwei Typen wählen:</p>
                
                <p><strong>📤 Verkauf:</strong></p>
                <p>Verwenden Sie diesen Typ, wenn Ware verkauft oder ausgegeben wird. Diese Scans werden grün markiert.</p>

                <p><strong>📥 Lagern (Einkauf):</strong></p>
                <p>Verwenden Sie diesen Typ, wenn Ware eingekauft oder ins Lager aufgenommen wird. Diese Scans werden blau markiert.</p>

                <div class="tip-box">
                    <strong>💡 Tipp:</strong> Die Statistiken auf der Startseite zeigen Ihnen die Gesamtanzahl von Verkaufs- und Lager-Scans.
                </div>
            </div>
        </div>

        <div class="help-card">
            <div class="help-title">
                <div class="help-icon">✓</div>
                <span>Eigenbedarf markieren</span>
            </div>
            <div class="help-content">
                <p>Beim Erfassen eines Scans können Sie die Option "Eigenbedarf" aktivieren.</p>
                
                <p><strong>Wann verwenden:</strong></p>
                <ul>
                    <li>Wenn Ware für den internen Gebrauch entnommen wird</li>
                    <li>Für Musterstücke oder Testprodukte</li>
                    <li>Bei Entnahme für Firmenzwecke</li>
                </ul>

                <p>Eigenbedarfs-Scans werden in der Liste speziell gekennzeichnet.</p>
            </div>
        </div>

        <div class="help-card">
            <div class="help-title">
                <div class="help-icon">🔍</div>
                <span>Suchen & Filtern</span>
            </div>
            <div class="help-content">
                <p>In der Scan-Liste können Sie Ihre Daten durchsuchen und filtern:</p>
                
                <p><strong>Suchfunktion:</strong></p>
                <p>Geben Sie einen Suchbegriff ein, um nach Artikelnummer, Name, Preis oder anderen Feldern zu suchen. Die Liste wird automatisch gefiltert.</p>

                <p><strong>Filter-Buttons:</strong></p>
                <ul>
                    <li><strong>Alle:</strong> Zeigt alle Scans an</li>
                    <li><strong>Verkauf:</strong> Zeigt nur Verkaufs-Scans</li>
                    <li><strong>Lagern:</strong> Zeigt nur Lager-Scans</li>
                </ul>

                <p><strong>CSV-Export:</strong></p>
                <p>Klicken Sie auf "Export CSV", um die aktuell sichtbaren Daten als CSV-Datei herunterzuladen. Diese kann in Excel oder anderen Programmen geöffnet werden.</p>
            </div>
        </div>

        <div class="help-card">
            <div class="help-title">
                <div class="help-icon">⚠️</div>
                <span>Daten löschen</span>
            </div>
            <div class="help-content">
                <p>Sie können alle gespeicherten Scan-Daten auf einmal löschen.</p>
                
                <div class="warning-box">
                    <strong>⚠️ Achtung:</strong> Das Löschen der Daten kann nicht rückgängig gemacht werden! Erstellen Sie bei Bedarf vorher einen CSV-Export als Backup.
                </div>

                <p><strong>So löschen Sie Daten:</strong></p>
                <ul>
                    <li>Klicken Sie auf "Daten löschen" im Menü</li>
                    <li>Bestätigen Sie die Sicherheitsabfrage</li>
                    <li>Alle Daten werden permanent entfernt</li>
                </ul>
            </div>
        </div>

        <div class="help-card">
            <div class="help-title">
                <div class="help-icon">💾</div>
                <span>Datenspeicherung</span>
            </div>
            <div class="help-content">
                <p>Alle Scan-Daten werden lokal auf dem Server in einer JSON-Datei gespeichert.</p>
                
                <p><strong>Gespeicherte Informationen:</strong></p>
                <ul>
                    <li>Zeitstempel (Datum und Uhrzeit des Scans)</li>
                    <li>Gescannte Daten (vollständiger Inhalt)</li>
                    <li>Menge</li>
                    <li>Typ (Verkauf oder Lagern)</li>
                    <li>Eigenbedarf (Ja/Nein)</li>
                </ul>

                <div class="tip-box">
                    <strong>💡 Tipp:</strong> Exportieren Sie regelmäßig Ihre Daten als CSV, um eine Sicherheitskopie zu haben.
                </div>
            </div>
        </div>

        <div class="links">
            <a href="start.php" class="back-link">🏠 Zurück zum Start</a>
        </div>
    </div>
</body>

</html>

<!doctype html>
<html lang="de">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Hilfe - Scanner System</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Sora:wght@400;600;700;800&display=swap');

        :root {
            --bg-top: #eef4ff;
            --bg-bottom: #dde8ff;
            --ink: #142033;
            --muted: #5a6b86;
            --surface: rgba(255, 255, 255, 0.88);
            --surface-strong: #ffffff;
            --border: rgba(20, 32, 51, 0.14);
            --shadow-soft: 0 12px 34px rgba(16, 36, 92, 0.10);
            --shadow-hover: 0 20px 46px rgba(16, 36, 92, 0.18);
            --brand: #2b63f6;
            --brand-dark: #214cc7;
            --warn-bg: #fff7e9;
            --warn-border: #b37217;
            --tip-bg: #edf3ff;
            --tip-border: #3c73ff;
            --radius-lg: 22px;
            --radius-md: 16px;
            --radius-sm: 12px;
        }

        @media (prefers-color-scheme: dark) {
            :root {
                --bg-top: #070e20;
                --bg-bottom: #040917;
                --ink: #e8eeff;
                --muted: #9fb1d6;
                --surface: rgba(16, 24, 43, 0.88);
                --surface-strong: #121b33;
                --border: rgba(149, 174, 226, 0.2);
                --shadow-soft: 0 12px 34px rgba(1, 8, 28, 0.45);
                --shadow-hover: 0 20px 46px rgba(1, 8, 28, 0.58);
                --brand: #6b95ff;
                --brand-dark: #9eb9ff;
                --warn-bg: #372913;
                --warn-border: #d39a4a;
                --tip-bg: #162a57;
                --tip-border: #7ea2ff;
            }
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body,
        button,
        input {
            font-family: "Sora", "Avenir Next", "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
        }

        body {
            min-height: 100vh;
            font-family: "Trebuchet MS", "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
            color: var(--ink);
            background:
                radial-gradient(950px 460px at 5% -10%, rgba(43, 99, 246, 0.18), transparent 60%),
                radial-gradient(760px 420px at 100% 0%, rgba(72, 168, 255, 0.2), transparent 55%),
                linear-gradient(180deg, var(--bg-top), var(--bg-bottom));
            padding: 14px;
            padding-bottom: calc(20px + env(safe-area-inset-bottom));
        }

        @media (prefers-color-scheme: dark) {
            body {
                background:
                    radial-gradient(950px 460px at 5% -10%, rgba(103, 139, 255, 0.22), transparent 60%),
                    radial-gradient(760px 420px at 100% 0%, rgba(34, 77, 153, 0.28), transparent 55%),
                    linear-gradient(180deg, var(--bg-top), var(--bg-bottom));
            }
        }

        .page {
            max-width: 980px;
            margin: 0 auto;
            display: grid;
            gap: 10px;
            animation: fadeIn 250ms ease-out;
        }

        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(8px);
            }

            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .header {
            display: flex;
            flex-direction: column;
            gap: 10px;
            align-items: flex-start;
            border: 1px solid var(--border);
            border-radius: var(--radius-lg);
            background: linear-gradient(145deg, rgba(255, 255, 255, 0.93), rgba(255, 255, 255, 0.76));
            box-shadow: var(--shadow-soft);
            padding: 14px;
        }

        @media (prefers-color-scheme: dark) {
            .header {
                background: linear-gradient(145deg, rgba(18, 27, 49, 0.95), rgba(12, 19, 38, 0.92));
            }
        }

        .vebo-logo {
            width: min(100%, 240px);
            height: auto;
            display: block;
            flex-shrink: 0;
        }

        h1 {
            font-size: clamp(1.3rem, 4.7vw, 2.2rem);
            line-height: 1.1;
            margin-bottom: 4px;
        }

        .subtitle {
            color: var(--muted);
            line-height: 1.45;
            max-width: 60ch;
            font-size: 0.9rem;
        }

        .help-card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-soft);
            padding: 14px;
            transition: transform 160ms ease, box-shadow 160ms ease;
        }

        .help-card:hover {
            transform: translateY(-1px);
            box-shadow: var(--shadow-hover);
        }

        .help-title {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            color: var(--brand-dark);
            margin-bottom: 8px;
        }

        .help-icon {
            width: 34px;
            height: 34px;
            border-radius: 8px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-size: 0.9rem;
            font-weight: 700;
            background: linear-gradient(135deg, var(--brand), #3c73ff);
            flex-shrink: 0;
        }

        .help-title h2 {
            font-size: 1rem;
            line-height: 1.3;
        }

        .help-content {
            color: var(--ink);
            line-height: 1.55;
            font-size: 0.92rem;
        }

        .help-content p + p {
            margin-top: 10px;
        }

        .help-content ul {
            margin: 8px 0 0 18px;
        }

        .help-content li + li {
            margin-top: 5px;
        }

        .code-example {
            margin-top: 10px;
            border: 1px solid var(--border);
            border-radius: 10px;
            background: rgba(241, 246, 255, 0.92);
            padding: 10px;
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
            font-size: 0.8rem;
            overflow-wrap: anywhere;
        }

        @media (prefers-color-scheme: dark) {
            .code-example {
                background: rgba(23, 33, 62, 0.9);
            }
        }

        .tip-box,
        .warning-box {
            margin-top: 10px;
            border-radius: 10px;
            padding: 10px;
            border: 1px solid transparent;
        }

        .tip-box {
            background: var(--tip-bg);
            border-color: var(--tip-border);
        }

        .warning-box {
            background: var(--warn-bg);
            border-color: var(--warn-border);
        }

        .links {
            display: flex;
            justify-content: center;
        }

        .back-link {
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 12px 14px;
            min-height: 46px;
            text-decoration: none;
            font-size: 0.9rem;
            font-weight: 700;
            color: var(--brand-dark);
            background: rgba(255, 255, 255, 0.86);
            box-shadow: var(--shadow-soft);
            transition: transform 160ms ease, box-shadow 160ms ease;
            width: 100%;
            text-align: center;
            touch-action: manipulation;
        }

        @media (prefers-color-scheme: dark) {
            .back-link {
                background: rgba(21, 30, 24, 0.92);
            }
        }

        .back-link:hover {
            transform: translateY(-1px);
            box-shadow: var(--shadow-hover);
        }

        @media (min-width: 760px) {
            body {
                padding: 20px;
                padding-bottom: calc(24px + env(safe-area-inset-bottom));
            }

            .page {
                gap: 14px;
            }

            .header {
                flex-direction: row;
                align-items: center;
                padding: 18px;
                gap: 14px;
            }

            .vebo-logo {
                width: min(320px, 100%);
            }

            .help-card {
                padding: 18px;
            }

            .help-title h2 {
                font-size: 1.2rem;
            }

            .help-content {
                font-size: 0.95rem;
            }

            .code-example {
                font-size: 0.86rem;
            }

            .back-link {
                width: auto;
            }
        }

        /* Visual polish layer */
        body::before,
        body::after {
            content: "";
            position: fixed;
            z-index: 0;
            pointer-events: none;
            opacity: 0.45;
            filter: blur(12px);
        }

        body::before {
            width: 300px;
            height: 300px;
            top: -80px;
            right: -90px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(74, 129, 255, 0.34), transparent 65%);
        }

        body::after {
            width: 320px;
            height: 320px;
            bottom: -120px;
            left: -120px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(47, 122, 255, 0.28), transparent 68%);
        }

        .page {
            position: relative;
            z-index: 1;
        }

        .header,
        .help-card,
        .back-link,
        .tip-box,
        .warning-box {
            backdrop-filter: blur(8px);
            -webkit-backdrop-filter: blur(8px);
        }

        .help-card {
            position: relative;
            overflow: hidden;
        }

        .help-card::after {
            content: "";
            position: absolute;
            inset: 0;
            pointer-events: none;
            background: linear-gradient(130deg, rgba(255, 255, 255, 0.12), transparent 40%, transparent 75%, rgba(43, 99, 246, 0.06));
        }

        .help-title h2 {
            letter-spacing: -0.01em;
        }

        @media (prefers-color-scheme: dark) {
            body::before {
                background: radial-gradient(circle, rgba(117, 167, 255, 0.3), transparent 65%);
            }

            body::after {
                background: radial-gradient(circle, rgba(90, 140, 255, 0.24), transparent 68%);
            }

            .help-card::after {
                background: linear-gradient(130deg, rgba(255, 255, 255, 0.05), transparent 40%, transparent 75%, rgba(124, 150, 255, 0.1));
            }
        }
    </style>
</head>

<body>
    <main class="page">
        <section class="header">
            <img src="logo.png" alt="VEBO Logo" class="vebo-logo">
            <div>
                <h1>Hilfe und Anleitung</h1>
                <p class="subtitle">Kurzanleitung fuer Scan, Datenerfassung, Suche, Export und Verwaltung.</p>
            </div>
        </section>

        <section class="help-card">
            <div class="help-title">
                <span class="help-icon">1</span>
                <h2>QR-Code und Barcode scannen</h2>
            </div>
            <div class="help-content">
                <p>Das System unterstuetzt das Scannen ueber die Kamera Ihres Geraets.</p>
                <ul>
                    <li>Auf der Startseite <strong>Neuer Scan</strong> oeffnen.</li>
                    <li>Auf <strong>Kamera starten</strong> klicken.</li>
                    <li>Code in die Kamera halten und kurz ruhig bleiben.</li>
                    <li>Der erkannte Wert wird automatisch uebernommen.</li>
                </ul>
                <div class="tip-box"><strong>Tipp:</strong> Gute Beleuchtung verbessert die Erkennungsrate deutlich.</div>
            </div>
        </section>

        <section class="help-card">
            <div class="help-title">
                <span class="help-icon">2</span>
                <h2>Datenformat und Felder</h2>
            </div>
            <div class="help-content">
                <p>Strukturierte Daten werden automatisch geparst.</p>
                <div class="code-example">artikelNr:001,name:Produkt,buyPrice:25.50;sellPrice:28.80;vendor:Lieferant</div>
                <p>Erkannte Schluessel sind z. B. <strong>artikelNr</strong>, <strong>name</strong>, <strong>buyPrice</strong>, <strong>sellPrice</strong> und <strong>vendor</strong>.</p>
            </div>
        </section>

        <section class="help-card">
            <div class="help-title">
                <span class="help-icon">3</span>
                <h2>Scan-Typen</h2>
            </div>
            <div class="help-content">
                <ul>
                    <li><strong>Verkauf:</strong> Ausgabe oder Verkauf von Ware.</li>
                    <li><strong>Lagern:</strong> Einlagerung oder Einkauf.</li>
                    <li><strong>Bestellen:</strong> Bedarf fuer Nachbestellung erfassen.</li>
                </ul>
            </div>
        </section>

        <section class="help-card">
            <div class="help-title">
                <span class="help-icon">4</span>
                <h2>Eigenbedarf</h2>
            </div>
            <div class="help-content">
                <p>Aktivieren Sie <strong>Eigenbedarf</strong>, wenn Ware fuer interne Zwecke verwendet wird.</p>
                <p>In der Liste bleibt diese Information beim jeweiligen Eintrag sichtbar.</p>
            </div>
        </section>

        <section class="help-card">
            <div class="help-title">
                <span class="help-icon">5</span>
                <h2>Suchen, filtern und exportieren</h2>
            </div>
            <div class="help-content">
                <p>In der Scan-Liste koennen Sie per Suchfeld filtern und mit Typ-Buttons eingrenzen.</p>
                <p>Mit <strong>Export CSV</strong> werden die aktuell sichtbaren Eintraege als Datei gespeichert.</p>
            </div>
        </section>

        <section class="help-card">
            <div class="help-title">
                <span class="help-icon">6</span>
                <h2>Daten loeschen</h2>
            </div>
            <div class="help-content">
                <p>Der Menuepunkt <strong>Daten loeschen</strong> leert alle gespeicherten Scans.</p>
                <div class="warning-box"><strong>Achtung:</strong> Dieser Schritt ist dauerhaft. Erstellen Sie vorher bei Bedarf einen CSV-Export.</div>
            </div>
        </section>

        <div class="links">
            <a href="start.php" class="back-link">Zurueck zum Start</a>
        </div>
    </main>
</body>

</html>

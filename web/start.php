<?php
// Define JSON file path
$jsonFile = 'scans.json';

// Get statistics
$totalScans = 0;
$recentScans = [];

if (file_exists($jsonFile)) {
    $jsonContent = file_get_contents($jsonFile);
    $scans = json_decode($jsonContent, true) ?? [];
    if (is_array($scans)) {
        $totalScans = count($scans);
        $recentScans = array_slice(array_reverse($scans), 0, 3);
    }
}
?>
<!doctype html>
<html lang="de">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Scanner - Start</title>
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
            --shadow-hover: 0 20px 46px rgba(16, 36, 92, 0.2);
            --brand: #2b63f6;
            --brand-dark: #214cc7;
            --warn: #c0392b;
            --buy: #1f8bff;
            --sell: #4c6dff;
            --order: #6a5cff;
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
                --buy: #57b1ff;
                --sell: #7f93ff;
                --order: #9b88ff;
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
            padding-bottom: calc(18px + env(safe-area-inset-bottom));
        }

        @media (prefers-color-scheme: dark) {
            body {
                background:
                    radial-gradient(950px 460px at 5% -10%, rgba(103, 139, 255, 0.22), transparent 60%),
                    radial-gradient(760px 420px at 100% 0%, rgba(34, 77, 153, 0.28), transparent 55%),
                    linear-gradient(180deg, var(--bg-top), var(--bg-bottom));
            }
        }

        .container {
            max-width: 980px;
            margin: 0 auto;
            animation: fadeIn 260ms ease-out;
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

        .hero {
            background: linear-gradient(145deg, rgba(255, 255, 255, 0.93), rgba(255, 255, 255, 0.76));
            border: 1px solid var(--border);
            border-radius: var(--radius-lg);
            padding: 16px;
            box-shadow: var(--shadow-soft);
            margin-bottom: 12px;
            display: flex;
            align-items: flex-start;
            gap: 12px;
            flex-direction: column;
        }

        @media (prefers-color-scheme: dark) {
            .hero {
                background: linear-gradient(145deg, rgba(18, 27, 49, 0.95), rgba(12, 19, 38, 0.92));
            }
        }

        .vebo-logo {
            width: min(100%, 250px);
            height: auto;
            display: block;
            flex-shrink: 0;
        }

        .hero-copy {
            flex: 1;
            min-width: 0;
        }

        h1 {
            font-size: clamp(1.45rem, 5vw, 2.3rem);
            line-height: 1.1;
            letter-spacing: -0.02em;
            margin-bottom: 6px;
        }

        .subtitle {
            color: var(--muted);
            font-size: 0.95rem;
            line-height: 1.45;
        }

        .panel {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-soft);
            padding: 14px;
        }

        .stats-card {
            margin-bottom: 12px;
        }

        .stats-header,
        .recent-header {
            font-size: 1.1rem;
            font-weight: 700;
            margin-bottom: 14px;
            color: var(--brand-dark);
        }

        .stats-content {
            display: grid;
            gap: 14px;
            grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
        }

        .stat-item {
            background: var(--surface-strong);
            border: 1px solid var(--border);
            border-radius: var(--radius-sm);
            padding: 14px 16px;
            text-align: center;
        }

        .stat-number {
            font-size: clamp(1.7rem, 2.3vw + 0.8rem, 2.4rem);
            font-weight: 700;
            color: var(--brand);
            margin-bottom: 4px;
        }

        .stat-label {
            color: var(--muted);
            text-transform: uppercase;
            font-size: 0.78rem;
            letter-spacing: 0.08em;
        }

        .menu-grid {
            display: grid;
            grid-template-columns: 1fr;
            gap: 10px;
            margin-bottom: 12px;
        }

        .menu-card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            text-decoration: none;
            color: inherit;
            padding: 14px;
            display: grid;
            grid-template-columns: 48px 1fr;
            gap: 10px;
            align-items: center;
            transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
            box-shadow: var(--shadow-soft);
            min-height: 90px;
            touch-action: manipulation;
        }

        .menu-card:hover,
        .menu-card:focus-visible {
            transform: translateY(-2px);
            box-shadow: var(--shadow-hover);
            border-color: rgba(31, 91, 48, 0.4);
            outline: none;
        }

        .menu-card:active {
            transform: translateY(0);
        }

        .menu-icon {
            width: 46px;
            height: 46px;
            border-radius: 10px;
            font-size: 1.2rem;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            background: linear-gradient(135deg, var(--brand), #3c73ff);
        }

        .menu-card.warn .menu-icon {
            background: linear-gradient(135deg, #c0392b, #d35400);
        }

        .menu-title {
            font-size: 1.02rem;
            font-weight: 700;
            color: var(--brand-dark);
        }

        .menu-description {
            font-size: 0.88rem;
            color: var(--muted);
            line-height: 1.35;
        }

        .recent-scans {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-soft);
            padding: 14px;
        }

        .recent-item {
            border: 1px solid var(--border);
            border-radius: var(--radius-sm);
            background: var(--surface-strong);
            padding: 12px;
            display: flex;
            align-items: flex-start;
            gap: 10px;
            justify-content: space-between;
            flex-direction: column;
        }

        .recent-item+.recent-item {
            margin-top: 10px;
        }

        .recent-content {
            flex: 1;
            min-width: 0;
        }

        .recent-time {
            color: var(--muted);
            font-size: 0.88rem;
            margin-bottom: 8px;
        }

        .recent-data {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(125px, 1fr));
            gap: 8px 10px;
        }

        .recent-field {
            padding: 8px;
            border-radius: 10px;
            border: 1px solid rgba(31, 42, 31, 0.08);
            background: rgba(248, 250, 247, 0.95);
        }

        @media (prefers-color-scheme: dark) {
            .recent-field {
                border-color: rgba(149, 174, 226, 0.16);
                background: rgba(23, 33, 62, 0.9);
            }
        }

        .recent-field-label {
            color: #6e7d9d;
            font-size: 0.7rem;
            text-transform: uppercase;
            letter-spacing: 0.06em;
            margin-bottom: 3px;
        }

        @media (prefers-color-scheme: dark) {
            .recent-field-label {
                color: #9fb0a2;
            }
        }

        .recent-field-value {
            color: var(--ink);
            font-size: 0.92rem;
            font-weight: 600;
            overflow-wrap: anywhere;
        }

        .recent-badge {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 86px;
            padding: 8px 12px;
            border-radius: 999px;
            font-size: 0.78rem;
            font-weight: 700;
            color: #fff;
            white-space: nowrap;
            align-self: flex-start;
        }

        .badge-sell {
            background: linear-gradient(135deg, var(--sell), #2e964a);
        }

        .badge-buy {
            background: linear-gradient(135deg, var(--buy), #2f8ade);
        }

        .badge-order {
            background: linear-gradient(135deg, var(--order), #b37217);
        }

        .no-data {
            color: var(--muted);
            text-align: center;
            padding: 16px;
        }

        @media (min-width: 680px) {
            .menu-grid {
                grid-template-columns: repeat(2, minmax(0, 1fr));
            }

            .menu-card {
                grid-template-columns: 1fr;
                align-items: start;
                min-height: 0;
            }
        }

        @media (min-width: 760px) {
            body {
                padding: 20px;
                padding-bottom: calc(24px + env(safe-area-inset-bottom));
            }

            .hero {
                flex-direction: row;
                align-items: center;
                padding: 22px;
                gap: 18px;
            }

            .vebo-logo {
                width: min(45vw, 340px);
            }

            .recent-item {
                flex-direction: row;
                align-items: center;
            }
        }

        @media (min-width: 1024px) {
            .menu-grid {
                grid-template-columns: repeat(4, minmax(0, 1fr));
            }
        }

        /* Visual polish layer */
        body::before,
        body::after {
            content: "";
            position: fixed;
            z-index: 0;
            pointer-events: none;
            filter: blur(12px);
            opacity: 0.45;
        }

        body::before {
            width: 280px;
            height: 280px;
            top: -70px;
            right: -80px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(74, 129, 255, 0.34), transparent 65%);
        }

        body::after {
            width: 300px;
            height: 300px;
            bottom: -120px;
            left: -120px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(47, 122, 255, 0.28), transparent 68%);
        }

        .container {
            position: relative;
            z-index: 1;
        }

        .hero,
        .panel,
        .recent-scans,
        .menu-card {
            backdrop-filter: blur(8px);
            -webkit-backdrop-filter: blur(8px);
        }

        .hero {
            border: 1px solid rgba(255, 255, 255, 0.35);
        }

        .menu-card {
            position: relative;
            overflow: hidden;
        }

        .menu-card::after {
            content: "";
            position: absolute;
            inset: 0;
            background: linear-gradient(125deg, rgba(255, 255, 255, 0.16), transparent 35%, transparent 65%, rgba(43, 99, 246, 0.08));
            pointer-events: none;
        }

        .menu-title {
            letter-spacing: -0.01em;
        }

        .menu-icon {
            box-shadow: 0 10px 18px rgba(39, 72, 166, 0.32);
        }

        .stat-number {
            text-shadow: 0 6px 16px rgba(43, 99, 246, 0.2);
        }

        @media (prefers-color-scheme: dark) {
            body::before {
                background: radial-gradient(circle, rgba(117, 167, 255, 0.3), transparent 65%);
            }

            body::after {
                background: radial-gradient(circle, rgba(90, 140, 255, 0.25), transparent 68%);
            }

            .hero {
                border-color: rgba(150, 184, 160, 0.35);
            }

            .menu-card::after {
                background: linear-gradient(125deg, rgba(255, 255, 255, 0.06), transparent 35%, transparent 65%, rgba(124, 150, 255, 0.1));
            }
        }
    </style>
</head>

<body>
    <div class="container">
        <div class="hero">
            <img src="logo.png" alt="VEBO Logo" class="vebo-logo">
            <div class="hero-copy">
                <h1>Scanner System</h1>
                <p class="subtitle">Willkommen beim Scan-Verwaltungssystem</p>
            </div>
        </div>

        <div class="panel stats-card">
            <div class="stats-header">Statistiken</div>
            <div class="stats-content">
                <div class="stat-item">
                    <div class="stat-number"><?php echo $totalScans; ?></div>
                    <div class="stat-label">Gesamt Scans</div>
                </div>
            </div>
        </div>

        <div class="menu-grid">
            <a href="scan.php" class="menu-card">
                <div class="menu-icon">+</div>
                <div class="menu-title">Neuer Scan</div>
                <div class="menu-description">Erfassen Sie neue Scan-Daten</div>
            </a>

            <a href="list.php" class="menu-card">
                <div class="menu-icon">=</div>
                <div class="menu-title">Scan Liste</div>
                <div class="menu-description">Alle erfassten Scans anzeigen</div>
            </a>

            <a href="delete.php" class="menu-card warn" onclick="return confirm('Moechten Sie wirklich alle Daten loeschen?')">
                <div class="menu-icon">X</div>
                <div class="menu-title">Daten loeschen</div>
                <div class="menu-description">Alle gespeicherten Daten entfernen</div>
            </a>

            <a href="help.php" class="menu-card">
                <div class="menu-icon">?</div>
                <div class="menu-title">Hilfe</div>
                <div class="menu-description">Anleitung und Informationen</div>
            </a>
        </div>

        <?php if (!empty($recentScans)) { ?>
            <div class="recent-scans">
                <div class="recent-header">Letzte Scans</div>
                <?php foreach ($recentScans as $scan) { ?>
                    <?php
                    // Parse structured data
                    $dataStr = $scan['data'];
                    $parsedData = [];

                    // Split by comma or semicolon
                    $parts = preg_split('/[,;]/', $dataStr);
                    foreach ($parts as $part) {
                        if (strpos($part, ':') !== false) {
                            list($key, $value) = explode(':', $part, 2);
                            $parsedData[trim($key)] = trim($value);
                        }
                    }
                    ?>
                    <div class="recent-item">
                        <div class="recent-content">
                            <div class="recent-time"><?php echo htmlspecialchars($scan['timestamp']); ?></div>
                            <div class="recent-data">
                                <?php if (!empty($parsedData)) { ?>
                                    <?php if (isset($parsedData['artikelNr'])) { ?>
                                        <div class="recent-field">
                                            <div class="recent-field-label">Artikel-Nr</div>
                                            <div class="recent-field-value"><?php echo htmlspecialchars($parsedData['artikelNr']); ?></div>
                                        </div>
                                    <?php } ?>
                                    <?php if (isset($parsedData['name'])) { ?>
                                        <div class="recent-field">
                                            <div class="recent-field-label">Name</div>
                                            <div class="recent-field-value"><?php echo htmlspecialchars($parsedData['name']); ?></div>
                                        </div>
                                    <?php } ?>
                                    <?php if (isset($parsedData['sellPrice']) || isset($parsedData['buyPrice'])) { ?>
                                        <div class="recent-field">
                                            <div class="recent-field-label">Preis</div>
                                            <div class="recent-field-value">
                                                <?php if (isset($parsedData['sellPrice'])) { ?>
                                                    <?php echo htmlspecialchars($parsedData['sellPrice']); ?> CHF
                                                <?php } elseif (isset($parsedData['buyPrice'])) { ?>
                                                    <?php echo htmlspecialchars($parsedData['buyPrice']); ?> CHF
                                                <?php } ?>
                                            </div>
                                        </div>
                                    <?php } ?>
                                    <div class="recent-field">
                                        <div class="recent-field-label">Menge</div>
                                        <div class="recent-field-value"><?php echo htmlspecialchars($scan['quantity']); ?></div>
                                    </div>
                                <?php } else { ?>
                                    <div class="recent-field">
                                        <div class="recent-field-label">Daten</div>
                                        <div class="recent-field-value"><?php echo htmlspecialchars($scan['data']); ?></div>
                                    </div>
                                    <div class="recent-field">
                                        <div class="recent-field-label">Menge</div>
                                        <div class="recent-field-value"><?php echo htmlspecialchars($scan['quantity']); ?></div>
                                    </div>
                                <?php } ?>
                            </div>
                        </div>
                        <span class="recent-badge badge-<?php echo htmlspecialchars($scan['type']); ?>">
                            <?php echo ($scan['type'] === 'sell') ? 'Verkauf' : (($scan['type'] === 'buy') ? 'Lagern' : 'Bestellen'); ?>
                        </span>
                    </div>
                <?php } ?>
            </div>
        <?php } else { ?>
            <div class="recent-scans">
                <div class="recent-header">Letzte Scans</div>
                <div class="no-data">Noch keine Scans vorhanden</div>
            </div>
        <?php } ?>
    </div>
</body>

</html>

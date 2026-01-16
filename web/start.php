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
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }

        .container {
            max-width: 900px;
            width: 100%;
        }

        .header {
            text-align: center;
            margin-bottom: 40px;
            position: relative;
        }

        .logo {
            font-size: 128px;
            margin-bottom: 15px;
            animation: fadeInDown 0.6s ease-out, float 3s ease-in-out infinite;
            display: inline-block;
        }

        @keyframes float {
            0%, 100% {
                transform: translateY(0);
            }
            50% {
                transform: translateY(-10px);
            }
        }

        h1 {
            color: white;
            font-size: 48px;
            margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
            font-weight: 700;
            letter-spacing: -1px;
            animation: fadeInDown 0.6s ease-out 0.1s both;
        }

        @keyframes fadeInDown {
            from {
                opacity: 0;
                transform: translateY(-20px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .subtitle {
            color: rgba(255, 255, 255, 0.9);
            font-size: 18px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
        }

        .menu-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .menu-card:nth-child(1) {
            animation: fadeInUp 0.6s ease-out 0.3s both;
        }

        .menu-card:nth-child(2) {
            animation: fadeInUp 0.6s ease-out 0.4s both;
        }

        .menu-card:nth-child(3) {
            animation: fadeInUp 0.6s ease-out 0.5s both;
        }

        .menu-card:nth-child(4) {
            animation: fadeInUp 0.6s ease-out 0.6s both;
        }

        .menu-card {
            background: white;
            padding: 30px;
            border-radius: 16px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
            text-align: center;
            text-decoration: none;
            color: #333;
            transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            cursor: pointer;
            position: relative;
            overflow: hidden;
        }

        .menu-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.1), transparent);
            transition: left 0.5s;
        }

        .menu-card:hover::before {
            left: 100%;
        }

        .menu-card:hover {
            transform: translateY(-8px) scale(1.02);
            box-shadow: 0 15px 45px rgba(102, 126, 234, 0.3);
        }

        .menu-card:hover .menu-icon {
            transform: rotate(10deg) scale(1.1);
        }

        .menu-card:active {
            transform: translateY(-4px) scale(0.98);
        }

        .menu-icon {
            font-size: 48px;
            margin-bottom: 15px;
            height: 80px;
            width: 80px;
            margin: 0 auto 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        }

        .menu-title {
            font-size: 24px;
            font-weight: 600;
            margin-bottom: 10px;
            color: #667eea;
        }

        .menu-description {
            color: #666;
            font-size: 14px;
        }

        .stats-card {
            background: white;
            padding: 25px;
            border-radius: 16px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
            margin-bottom: 30px;
            animation: fadeInUp 0.6s ease-out 0.2s both;
        }

        @keyframes fadeInUp {
            from {
                opacity: 0;
                transform: translateY(20px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .stats-header {
            font-size: 20px;
            font-weight: 600;
            color: #667eea;
            margin-bottom: 20px;
            text-align: center;
        }

        .stats-content {
            display: flex;
            justify-content: space-around;
            flex-wrap: wrap;
            gap: 20px;
        }

        .stat-item {
            text-align: center;
        }

        .stat-number {
            font-size: 48px;
            font-weight: bold;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            margin-bottom: 5px;
            animation: countUp 1s ease-out;
        }

        @keyframes countUp {
            from {
                opacity: 0;
                transform: scale(0.5);
            }
            to {
                opacity: 1;
                transform: scale(1);
            }
        }

        .stat-label {
            color: #666;
            font-size: 14px;
            text-transform: uppercase;
        }

        .recent-scans {
            background: white;
            padding: 25px;
            border-radius: 16px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
            animation: fadeInUp 0.6s ease-out 0.6s both;
        }

        .recent-header {
            font-size: 20px;
            font-weight: 600;
            color: #667eea;
            margin-bottom: 15px;
        }

        .recent-item {
            padding: 15px;
            border-bottom: 1px solid #f0f0f0;
            display: flex;
            justify-content: space-between;
            align-items: center;
            transition: all 0.3s ease;
            border-radius: 8px;
        }

        .recent-item:hover {
            background: #f8f9ff;
            transform: translateX(5px);
            padding-left: 20px;
        }

        .recent-item:last-child {
            border-bottom: none;
        }

        .recent-content {
            flex: 1;
        }

        .recent-data {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
            gap: 10px;
            margin-top: 8px;
        }

        .recent-field {
            font-size: 13px;
        }

        .recent-field-label {
            color: #999;
            font-size: 11px;
            text-transform: uppercase;
        }

        .recent-field-value {
            color: #333;
            font-weight: 600;
        }

        .recent-time {
            color: #666;
            font-size: 14px;
        }

        .recent-badge {
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            transition: all 0.3s ease;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .recent-item:hover .recent-badge {
            transform: scale(1.05);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }

        .badge-sell {
            background: #4caf50;
            color: white;
        }

        .badge-buy {
            background: #2196f3;
            color: white;
        }

        .no-data {
            text-align: center;
            color: #999;
            padding: 20px;
        }

        .vebo-logo {
            max-width: 400px;
            height: auto;
            margin: auto;
            display: block;
            animation: fadeInDown 0.6s ease-out;
            filter: drop-shadow(2px 2px 4px rgba(0, 0, 0, 0.2));
        }

        @media (max-width: 768px) {
            h1 {
                font-size: 36px;
            }

            .menu-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>

<body>
    <div class="container">
        <div class="header">
            <img src="logo.png" alt="VEBO Logo" class="vebo-logo">
            <h1>Scanner System</h1>
            <p class="subtitle">Willkommen beim Scan-Verwaltungssystem</p>
        </div>

        <div class="stats-card">
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

            <a href="delete.php" class="menu-card" onclick="return confirm('Moechten Sie wirklich alle Daten loeschen?')">
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
                            <?php echo ($scan['type'] === 'sell') ? 'Verkauf' : 'Lagern'; ?>
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

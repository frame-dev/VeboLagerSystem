<?php
// Define JSON file path
$jsonFile = 'scans.json';

// Read existing data from JSON file
$scans = [];
if (file_exists($jsonFile)) {
    $jsonContent = file_get_contents($jsonFile);
    $scans = json_decode($jsonContent, true);
    if (!is_array($scans)) {
        $scans = [];
    }
}

// Calculate statistics
$totalScans = count($scans);
$totalSell = 0;
$totalBuy = 0;
$totalQuantity = 0;

foreach ($scans as $scan) {
    $totalQuantity += (int)$scan['quantity'];
    if ($scan['type'] === 'sell') {
        $totalSell += (int)$scan['quantity'];
    } else {
        $totalBuy += (int)$scan['quantity'];
    }
}
?>
<!doctype html>
<html lang="de">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Scan Liste</title>
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

        @keyframes slideInLeft {
            from {
                opacity: 0;
                transform: translateX(-30px);
            }
            to {
                opacity: 1;
                transform: translateX(0);
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
            max-width: 800px;
            margin: 0 auto;
            position: relative;
            z-index: 1;
        }

        h1 {
            color: white;
            margin-bottom: 30px;
            font-size: 32px;
            text-align: center;
            text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
            animation: slideDown 0.6s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .scan-item {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 25px;
            border-radius: 16px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            margin-bottom: 20px;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            animation: fadeIn 0.5s cubic-bezier(0.4, 0, 0.2, 1) backwards;
        }

        .scan-item:nth-child(1) { animation-delay: 0.05s; }
        .scan-item:nth-child(2) { animation-delay: 0.1s; }
        .scan-item:nth-child(3) { animation-delay: 0.15s; }
        .scan-item:nth-child(4) { animation-delay: 0.2s; }
        .scan-item:nth-child(5) { animation-delay: 0.25s; }

        .scan-item:hover {
            transform: translateY(-5px) scale(1.01);
            box-shadow: 0 15px 50px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
        }

        .scan-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 2px solid #f0f0f0;
        }

        .timestamp {
            color: #666;
            font-size: 14px;
        }

        .type-badge {
            padding: 8px 18px;
            border-radius: 25px;
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .scan-item:hover .type-badge {
            transform: scale(1.05);
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.25);
        }

        .type-sell {
            background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
            color: white;
        }

        .type-buy {
            background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%);
            color: white;
        }

        .scan-details {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 15px;
        }

        .detail-item {
            display: flex;
            flex-direction: column;
            padding: 12px;
            background: #f8f9ff;
            border-radius: 10px;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .detail-item:hover {
            background: #eef1ff;
            transform: translateY(-2px);
        }

        .detail-label {
            color: #667eea;
            font-size: 11px;
            text-transform: uppercase;
            margin-bottom: 6px;
            font-weight: 600;
            letter-spacing: 0.5px;
        }

        .detail-value {
            color: #333;
            font-size: 16px;
            font-weight: 600;
        }

        .empty-state {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 50px 40px;
            border-radius: 16px;
            text-align: center;
            color: #666;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .empty-state h2 {
            margin-bottom: 15px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
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
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.3s backwards;
        }

        .stats-container {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.1s backwards;
        }

        .stat-card {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 25px 20px;
            border-radius: 16px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            text-align: center;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            overflow: hidden;
        }

        .stat-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 4px;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            transform: scaleX(0);
            transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .stat-card:hover::before {
            transform: scaleX(1);
        }

        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 48px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
        }

        .stat-number {
            font-size: 42px;
            font-weight: bold;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            margin-bottom: 8px;
            animation: countUp 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.3s backwards;
        }

        .stat-label {
            color: #666;
            font-size: 14px;
            text-transform: uppercase;
        }

        .search-box {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 25px;
            border-radius: 16px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            margin-bottom: 30px;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.2s backwards;
        }

        .search-box input {
            width: 100%;
            padding: 14px 18px;
            border: 2px solid #e0e0e0;
            border-radius: 10px;
            font-size: 16px;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            background: #fafafa;
        }

        .search-box input:focus {
            outline: none;
            border-color: #667eea;
            background: white;
            box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1), 0 4px 12px rgba(102, 126, 234, 0.15);
            transform: translateY(-2px);
        }

        .filter-buttons {
            display: flex;
            gap: 12px;
            margin-top: 18px;
            flex-wrap: wrap;
        }

        .filter-btn {
            padding: 10px 24px;
            border: 2px solid #667eea;
            background: white;
            color: #667eea;
            border-radius: 10px;
            cursor: pointer;
            font-weight: 600;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            overflow: hidden;
        }

        .filter-btn::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            width: 0;
            height: 0;
            border-radius: 50%;
            background: rgba(102, 126, 234, 0.1);
            transform: translate(-50%, -50%);
            transition: width 0.6s, height 0.6s;
        }

        .filter-btn:hover::before {
            width: 300px;
            height: 300px;
        }

        .filter-btn:hover {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-color: transparent;
            transform: translateY(-2px);
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        }

        .filter-btn.active {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-color: transparent;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        }

        .export-btn {
            display: inline-block;
            padding: 14px 35px;
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            color: #667eea;
            text-decoration: none;
            border-radius: 10px;
            font-weight: 600;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            border: none;
            cursor: pointer;
            margin-left: 10px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
            position: relative;
            overflow: hidden;
        }

        .export-btn::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.2), transparent);
            transition: left 0.5s;
        }

        .export-btn:hover::before {
            left: 100%;
        }

        .export-btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 30px rgba(0, 0, 0, 0.25);
            background: rgba(255, 255, 255, 1);
        }

        .scan-number {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            font-size: 15px;
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .scan-item:hover .scan-number {
            transform: scale(1.1) rotate(5deg);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
        }

        .scan-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 2px solid #f0f0f0;
        }

        .header-left {
            display: flex;
            align-items: center;
            gap: 15px;
        }
    </style>
    <script>
        function filterScans(type) {
            const items = document.querySelectorAll('.scan-item');
            const buttons = document.querySelectorAll('.filter-btn');
            
            buttons.forEach(btn => btn.classList.remove('active'));
            event.target.classList.add('active');
            
            items.forEach(item => {
                if (type === 'all') {
                    item.style.display = 'block';
                } else {
                    const itemType = item.dataset.type;
                    item.style.display = itemType === type ? 'block' : 'none';
                }
            });
        }

        function searchScans() {
            const searchTerm = document.getElementById('searchInput').value.toLowerCase();
            const items = document.querySelectorAll('.scan-item');
            
            items.forEach(item => {
                const text = item.textContent.toLowerCase();
                item.style.display = text.includes(searchTerm) ? 'block' : 'none';
            });
        }

        function exportToCSV() {
            const items = document.querySelectorAll('.scan-item:not([style*="display: none"])');
            let csv = 'Zeitstempel,Daten,Menge,Typ,Eigenbedarf\n';
            
            items.forEach(item => {
                const timestamp = item.querySelector('.timestamp').textContent;
                const data = item.querySelector('.detail-value').textContent;
                const details = item.querySelectorAll('.detail-value');
                const quantity = details[1].textContent;
                const ownUse = details[2].textContent;
                const type = item.dataset.type;
                
                csv += `"${timestamp}","${data}","${quantity}","${type}","${ownUse}"\n`;
            });
            
            const blob = new Blob([csv], { type: 'text/csv' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'scans_export.csv';
            a.click();
            window.URL.revokeObjectURL(url);
        }
    </script>
</head>

<body>
    <div class="container">
        <h1>📊 Scan Liste</h1>
        
        <?php if (!empty($scans)): ?>
            <div class="stats-container">
                <div class="stat-card">
                    <div class="stat-number"><?php echo $totalScans; ?></div>
                    <div class="stat-label">Gesamt Scans</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number"><?php echo $totalSell; ?></div>
                    <div class="stat-label">Verkauf</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number"><?php echo $totalBuy; ?></div>
                    <div class="stat-label">Lagern</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number"><?php echo $totalQuantity; ?></div>
                    <div class="stat-label">Gesamt Menge</div>
                </div>
            </div>

            <div class="search-box">
                <input type="text" id="searchInput" placeholder="🔍 Suche..." oninput="searchScans()">
                <div class="filter-buttons">
                    <button class="filter-btn active" onclick="filterScans('all')">Alle</button>
                    <button class="filter-btn" onclick="filterScans('sell')">Verkauf</button>
                    <button class="filter-btn" onclick="filterScans('buy')">Lagern</button>
                </div>
            </div>
        <?php endif; ?>
        
        <div class="links">
            <a href="start.php" class="back-link">Zurück zum Start</a>
            <?php if (!empty($scans)): ?>
                <button onclick="exportToCSV()" class="export-btn">📥 Export CSV</button>
            <?php endif; ?>
        </div>

        <?php if (empty($scans)): ?>
            <div class="empty-state">
                <h2>📭 Keine Scans vorhanden</h2>
                <p>Es wurden noch keine Daten erfasst.</p>
            </div>
        <?php else: ?>
            <?php 
            $counter = $totalScans;
            foreach (array_reverse($scans) as $scan): 
            ?>
                <div class="scan-item" data-type="<?php echo htmlspecialchars($scan['type']); ?>">
                    <div class="scan-header">
                        <div class="header-left">
                            <div class="scan-number"><?php echo $counter--; ?></div>
                            <span class="timestamp"><?php echo htmlspecialchars($scan['timestamp']); ?></span>
                        </div>
                        <span class="type-badge type-<?php echo htmlspecialchars($scan['type']); ?>">
                            <?php echo $scan['type'] === 'sell' ? '📤 Verkauf' : '📥 Lagern'; ?>
                        </span>
                    </div>
                    <div class="scan-details">
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
                        
                        // Display parsed fields or raw data
                        if (!empty($parsedData)):
                        ?>
                            <?php if (isset($parsedData['artikelNr'])): ?>
                            <div class="detail-item">
                                <span class="detail-label">Artikel-Nr</span>
                                <span class="detail-value"><?php echo htmlspecialchars($parsedData['artikelNr']); ?></span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if (isset($parsedData['name'])): ?>
                            <div class="detail-item">
                                <span class="detail-label">Name</span>
                                <span class="detail-value"><?php echo htmlspecialchars($parsedData['name']); ?></span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if (isset($parsedData['buyPrice'])): ?>
                            <div class="detail-item">
                                <span class="detail-label">Einkaufspreis</span>
                                <span class="detail-value"><?php echo htmlspecialchars($parsedData['buyPrice']); ?> CHF</span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if (isset($parsedData['sellPrice'])): ?>
                            <div class="detail-item">
                                <span class="detail-label">Verkaufspreis</span>
                                <span class="detail-value"><?php echo htmlspecialchars($parsedData['sellPrice']); ?> CHF</span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if (isset($parsedData['vendor'])): ?>
                            <div class="detail-item">
                                <span class="detail-label">Lieferant</span>
                                <span class="detail-value"><?php echo htmlspecialchars($parsedData['vendor']); ?></span>
                            </div>
                            <?php endif; ?>
                        <?php else: ?>
                            <div class="detail-item">
                                <span class="detail-label">Daten</span>
                                <span class="detail-value"><?php echo htmlspecialchars($scan['data']); ?></span>
                            </div>
                        <?php endif; ?>
                        
                        <div class="detail-item">
                            <span class="detail-label">Menge</span>
                            <span class="detail-value"><?php echo htmlspecialchars($scan['quantity']); ?></span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">Eigenbedarf</span>
                            <span class="detail-value"><?php echo $scan['ownUse'] === 'Ja' ? '✓ Ja' : '✗ Nein'; ?></span>
                        </div>
                    </div>
                </div>
            <?php endforeach; ?>
        <?php endif; ?>
    </div>
</body>

</html>
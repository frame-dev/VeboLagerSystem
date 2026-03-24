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
$totalOrder = 0;
$totalQuantity = 0;

foreach ($scans as $scan) {
    $quantity = (int)($scan['quantity'] ?? 0);
    $type = $scan['type'] ?? '';

    $totalQuantity += $quantity;
    if ($type === 'sell') {
        $totalSell += $quantity;
    } elseif ($type === 'order') {
        $totalOrder += $quantity;
    } else {
        $totalBuy += $quantity;
    }
}

function e($value)
{
    return htmlspecialchars((string)$value, ENT_QUOTES, 'UTF-8');
}
?>
<!doctype html>
<html lang="de">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Scan Liste</title>
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
            padding-bottom: calc(22px + env(safe-area-inset-bottom));
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
            max-width: 1080px;
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

        .panel {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-soft);
            padding: 14px;
        }

        .header {
            border-radius: var(--radius-lg);
            background: linear-gradient(145deg, rgba(255, 255, 255, 0.93), rgba(255, 255, 255, 0.76));
        }

        @media (prefers-color-scheme: dark) {
            .header {
                background: linear-gradient(145deg, rgba(18, 27, 49, 0.95), rgba(12, 19, 38, 0.92));
            }
        }

        h1 {
            font-size: clamp(1.35rem, 4.7vw, 2.2rem);
            line-height: 1.15;
            letter-spacing: -0.01em;
            margin-bottom: 4px;
        }

        .subline {
            color: var(--muted);
            line-height: 1.4;
            font-size: 0.9rem;
        }

        .stats {
            display: grid;
            gap: 10px;
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        @media (max-width: 420px) {
            .stats {
                grid-template-columns: 1fr;
            }
        }

        .stat-card {
            border: 1px solid var(--border);
            border-radius: var(--radius-sm);
            background: var(--surface-strong);
            padding: 12px;
            text-align: center;
        }

        .stat-number {
            color: var(--brand);
            font-size: 1.65rem;
            font-weight: 700;
            line-height: 1.1;
            margin-bottom: 4px;
        }

        .stat-label {
            color: var(--muted);
            text-transform: uppercase;
            letter-spacing: 0.08em;
            font-size: 0.72rem;
        }

        .search-box {
            display: grid;
            gap: 10px;
        }

        #searchInput {
            width: 100%;
            border: 1px solid rgba(31, 42, 31, 0.2);
            border-radius: 10px;
            background: #fff;
            padding: 12px 12px;
            min-height: 46px;
            font-size: 0.96rem;
            color: var(--ink);
        }

        #searchInput:focus {
            outline: 2px solid rgba(43, 99, 246, 0.2);
            border-color: rgba(43, 99, 246, 0.45);
        }

        .filter-buttons {
            display: flex;
            gap: 8px;
            overflow-x: auto;
            scrollbar-width: thin;
            padding-bottom: 2px;
            -webkit-overflow-scrolling: touch;
        }

        .filter-btn,
        .btn,
        .export-btn {
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 10px 14px;
            min-height: 44px;
            cursor: pointer;
            font-size: 0.9rem;
            font-weight: 700;
            text-decoration: none;
            transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
            touch-action: manipulation;
        }

        .filter-btn {
            background: rgba(255, 255, 255, 0.8);
            color: var(--brand-dark);
            white-space: nowrap;
            flex: 0 0 auto;
        }

        @media (prefers-color-scheme: dark) {
            #searchInput {
                background: #121b16;
                border-color: rgba(149, 174, 226, 0.24);
            }

            .filter-btn {
                background: rgba(21, 30, 24, 0.9);
            }
        }

        .filter-btn.active,
        .filter-btn:hover {
            background: linear-gradient(135deg, var(--brand), #3c73ff);
            color: #fff;
            border-color: transparent;
            box-shadow: 0 8px 20px rgba(43, 99, 246, 0.25);
            transform: translateY(-1px);
        }

        .actions {
            display: flex;
            gap: 8px;
            flex-direction: column;
            justify-content: center;
        }

        .btn,
        .export-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: var(--brand-dark);
            background: rgba(255, 255, 255, 0.8);
            box-shadow: var(--shadow-soft);
            width: 100%;
        }

        @media (prefers-color-scheme: dark) {
            .btn,
            .export-btn {
                background: rgba(21, 30, 24, 0.9);
            }
        }

        .export-btn {
            border: none;
            font-family: inherit;
        }

        .btn:hover,
        .export-btn:hover {
            transform: translateY(-1px);
            box-shadow: var(--shadow-hover);
        }

        .empty-state {
            text-align: center;
            color: var(--muted);
            padding: 28px 16px;
        }

        .scan-list {
            display: grid;
            gap: 10px;
        }

        .scan-item {
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            background: var(--surface);
            box-shadow: var(--shadow-soft);
            padding: 12px;
        }

        .scan-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 10px;
            margin-bottom: 10px;
        }

        .header-left {
            display: inline-flex;
            align-items: center;
            gap: 10px;
            min-width: 0;
        }

        .scan-number {
            width: 34px;
            height: 34px;
            border-radius: 999px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 0.86rem;
            font-weight: 700;
            color: #fff;
            background: linear-gradient(135deg, var(--brand), #3c73ff);
            flex-shrink: 0;
        }

        .timestamp {
            color: var(--muted);
            font-size: 0.86rem;
            overflow-wrap: anywhere;
        }

        .type-badge {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 90px;
            padding: 7px 10px;
            border-radius: 999px;
            font-size: 0.74rem;
            font-weight: 700;
            color: #fff;
            white-space: nowrap;
        }

        .type-sell {
            background: linear-gradient(135deg, var(--sell), #2e964a);
        }

        .type-buy {
            background: linear-gradient(135deg, var(--buy), #2f8ade);
        }

        .type-order {
            background: linear-gradient(135deg, var(--order), #b37217);
        }

        .scan-details {
            display: grid;
            gap: 8px;
            grid-template-columns: 1fr;
        }

        .detail-item {
            border: 1px solid rgba(31, 42, 31, 0.08);
            border-radius: 10px;
            background: rgba(248, 250, 247, 0.95);
            padding: 9px;
        }

        @media (prefers-color-scheme: dark) {
            .detail-item {
                border-color: rgba(149, 174, 226, 0.16);
                background: rgba(23, 33, 62, 0.9);
            }
        }

        .detail-label {
            color: #6e7d9d;
            font-size: 0.68rem;
            text-transform: uppercase;
            letter-spacing: 0.06em;
            margin-bottom: 3px;
        }

        @media (prefers-color-scheme: dark) {
            .detail-label {
                color: #9fb0a2;
            }
        }

        .detail-value {
            color: var(--ink);
            font-size: 0.92rem;
            font-weight: 600;
            overflow-wrap: anywhere;
        }

        .download-section {
            text-align: center;
        }

        .download-title {
            color: var(--muted);
            margin-bottom: 10px;
            font-size: 0.95rem;
        }

        @media (min-width: 600px) {
            .actions {
                flex-direction: row;
            }

            .btn,
            .export-btn {
                width: auto;
            }

            .scan-details {
                grid-template-columns: repeat(2, minmax(0, 1fr));
            }
        }

        @media (max-width: 700px) {
            .scan-header {
                flex-direction: column;
                align-items: flex-start;
            }
        }

        @media (min-width: 860px) {
            body {
                padding: 20px;
                padding-bottom: calc(26px + env(safe-area-inset-bottom));
            }

            .page {
                gap: 14px;
            }

            .panel {
                padding: 18px;
            }

            .stats {
                grid-template-columns: repeat(5, minmax(0, 1fr));
            }

            .scan-header {
                flex-direction: row;
                align-items: center;
            }

            .scan-details {
                grid-template-columns: repeat(3, minmax(0, 1fr));
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
            top: -85px;
            right: -90px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(74, 129, 255, 0.34), transparent 65%);
        }

        body::after {
            width: 330px;
            height: 330px;
            bottom: -120px;
            left: -130px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(47, 122, 255, 0.28), transparent 68%);
        }

        .page {
            position: relative;
            z-index: 1;
        }

        .panel,
        .scan-item,
        .btn,
        .export-btn,
        .filter-btn {
            backdrop-filter: blur(8px);
            -webkit-backdrop-filter: blur(8px);
        }

        .scan-item {
            position: relative;
            overflow: hidden;
        }

        .scan-item::after {
            content: "";
            position: absolute;
            inset: 0;
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), transparent 35%, transparent 70%, rgba(43, 99, 246, 0.05));
            pointer-events: none;
        }

        .scan-number {
            box-shadow: 0 8px 16px rgba(39, 72, 166, 0.32);
        }

        .stat-number {
            text-shadow: 0 6px 16px rgba(43, 99, 246, 0.2);
        }

        @media (prefers-color-scheme: dark) {
            body::before {
                background: radial-gradient(circle, rgba(117, 167, 255, 0.3), transparent 65%);
            }

            body::after {
                background: radial-gradient(circle, rgba(90, 140, 255, 0.24), transparent 68%);
            }

            .scan-item::after {
                background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), transparent 35%, transparent 70%, rgba(124, 150, 255, 0.1));
            }
        }
    </style>
    <script>
        let activeFilter = 'all';

        function setActiveButton(button) {
            document.querySelectorAll('.filter-btn').forEach((btn) => btn.classList.remove('active'));
            if (button) {
                button.classList.add('active');
            }
        }

        function applyFilters() {
            const searchTerm = document.getElementById('searchInput') ? document.getElementById('searchInput').value.toLowerCase() : '';
            const items = document.querySelectorAll('.scan-item');

            items.forEach((item) => {
                const matchesFilter = activeFilter === 'all' || item.dataset.type === activeFilter;
                const matchesSearch = item.textContent.toLowerCase().includes(searchTerm);
                item.style.display = matchesFilter && matchesSearch ? 'block' : 'none';
            });
        }

        function filterScans(type, button) {
            activeFilter = type;
            setActiveButton(button);
            applyFilters();
        }

        function searchScans() {
            applyFilters();
        }

        function exportToCSV() {
            // Build export URL with current filters
            const searchTerm = document.getElementById('searchInput') ? document.getElementById('searchInput').value : '';
            const params = new URLSearchParams();
            
            if (activeFilter !== 'all') {
                params.append('type', activeFilter);
            }
            
            if (searchTerm) {
                params.append('search', searchTerm);
            }
            
            const url = 'export.php' + (params.toString() ? '?' + params.toString() : '');
            window.location.href = url;
        }

        document.addEventListener('DOMContentLoaded', () => {
            applyFilters();
        });
    </script>
</head>

<body>
    <main class="page">
        <section class="panel header">
            <h1>Scan Liste</h1>
            <p class="subline">Durchsuchen, filtern und exportieren Sie Ihre erfassten Scans.</p>
        </section>

        <?php if (!empty($scans)): ?>
            <section class="panel stats">
                <div class="stat-card">
                    <div class="stat-number"><?php echo $totalScans; ?></div>
                    <div class="stat-label">Gesamt</div>
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
                    <div class="stat-number"><?php echo $totalOrder; ?></div>
                    <div class="stat-label">Bestellen</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number"><?php echo $totalQuantity; ?></div>
                    <div class="stat-label">Menge</div>
                </div>
            </section>

            <section class="panel search-box">
                <input type="text" id="searchInput" placeholder="Suche nach Artikel, Name, Preis oder Datum" oninput="searchScans()">
                <div class="filter-buttons">
                    <button class="filter-btn active" onclick="filterScans('all', this)">Alle</button>
                    <button class="filter-btn" onclick="filterScans('sell', this)">Verkauf</button>
                    <button class="filter-btn" onclick="filterScans('buy', this)">Lagern</button>
                    <button class="filter-btn" onclick="filterScans('order', this)">Bestellen</button>
                </div>
            </section>
        <?php endif; ?>

        <section class="actions">
            <a href="start.php" class="btn">Zurueck zum Start</a>
            <?php if (!empty($scans)): ?>
                <button type="button" onclick="exportToCSV()" class="export-btn">Export CSV</button>
            <?php endif; ?>
        </section>

        <?php if (empty($scans)): ?>
            <section class="panel empty-state">
                <h2>Keine Scans vorhanden</h2>
                <p>Es wurden noch keine Daten erfasst.</p>
            </section>
        <?php else: ?>
            <section class="scan-list">
                <?php
                $counter = $totalScans;
                foreach (array_reverse($scans) as $scan):
                    $scanType = $scan['type'] ?? 'buy';
                    $scanData = $scan['data'] ?? '';
                    $scanQuantity = $scan['quantity'] ?? '';
                    $scanOwnUse = $scan['ownUse'] ?? 'Nein';
                    $scanTimestamp = $scan['timestamp'] ?? '';
                    $scanSize = $scan['size'] ?? '';
                    $scanColor = $scan['color'] ?? '';
                    $scanId = $scan['id'] ?? '';

                    $parsedData = [];
                    $parts = preg_split('/[,;]/', $scanData);
                    foreach ($parts as $part) {
                        if (strpos($part, ':') !== false) {
                            list($key, $value) = explode(':', $part, 2);
                            $parsedData[trim($key)] = trim($value);
                        }
                    }

                    $typeLabel = $scanType === 'sell' ? 'Verkauf' : ($scanType === 'buy' ? 'Lagern' : 'Bestellen');
                ?>
                    <article
                        class="scan-item"
                        data-type="<?php echo e($scanType); ?>"
                        data-timestamp="<?php echo e($scanTimestamp); ?>"
                        data-data="<?php echo e($scanData); ?>"
                        data-quantity="<?php echo e($scanQuantity); ?>"
                        data-ownuse="<?php echo e($scanOwnUse); ?>"
                        data-size="<?php echo e($scanSize); ?>"
                        data-color="<?php echo e($scanColor); ?>"
                        data-id="<?php echo e($scanId); ?>"
                    >
                        <div class="scan-header">
                            <div class="header-left">
                                <div class="scan-number"><?php echo $counter--; ?></div>
                                <span class="timestamp"><?php echo e($scanTimestamp); ?></span>
                            </div>
                            <span class="type-badge type-<?php echo e($scanType); ?>"><?php echo e($typeLabel); ?></span>
                        </div>

                        <div class="scan-details">
                            <?php if (!empty($parsedData)): ?>
                                <?php if (isset($parsedData['artikelNr'])): ?>
                                    <div class="detail-item">
                                        <div class="detail-label">Artikel-Nr</div>
                                        <div class="detail-value"><?php echo e($parsedData['artikelNr']); ?></div>
                                    </div>
                                <?php endif; ?>

                                <?php if (isset($parsedData['name'])): ?>
                                    <div class="detail-item">
                                        <div class="detail-label">Name</div>
                                        <div class="detail-value"><?php echo e($parsedData['name']); ?></div>
                                    </div>
                                <?php endif; ?>

                                <?php if (isset($parsedData['buyPrice'])): ?>
                                    <div class="detail-item">
                                        <div class="detail-label">Einkaufspreis</div>
                                        <div class="detail-value"><?php echo e($parsedData['buyPrice']); ?> CHF</div>
                                    </div>
                                <?php endif; ?>

                                <?php if (isset($parsedData['sellPrice'])): ?>
                                    <div class="detail-item">
                                        <div class="detail-label">Verkaufspreis</div>
                                        <div class="detail-value"><?php echo e($parsedData['sellPrice']); ?> CHF</div>
                                    </div>
                                <?php endif; ?>

                                <?php if (isset($parsedData['vendor'])): ?>
                                    <div class="detail-item">
                                        <div class="detail-label">Lieferant</div>
                                        <div class="detail-value"><?php echo e($parsedData['vendor']); ?></div>
                                    </div>
                                <?php endif; ?>
                            <?php else: ?>
                                <div class="detail-item">
                                    <div class="detail-label">Daten</div>
                                    <div class="detail-value"><?php echo e($scanData); ?></div>
                                </div>
                            <?php endif; ?>

                            <div class="detail-item">
                                <div class="detail-label">Menge</div>
                                <div class="detail-value"><?php echo e($scanQuantity); ?></div>
                            </div>
                            <?php if (!empty($scanSize)): ?>
                            <div class="detail-item">
                                <div class="detail-label">Größe</div>
                                <div class="detail-value"><?php echo e($scanSize); ?></div>
                            </div>
                            <?php endif; ?>
                            <?php if (!empty($scanColor)): ?>
                            <div class="detail-item">
                                <div class="detail-label">Farbe</div>
                                <div class="detail-value"><?php echo e($scanColor); ?></div>
                            </div>
                            <?php endif; ?>
                            <div class="detail-item">
                                <div class="detail-label">Eigenbedarf</div>
                                <div class="detail-value"><?php echo $scanOwnUse === 'Ja' ? 'Ja' : 'Nein'; ?></div>
                            </div>
                        </div>
                    </article>
                <?php endforeach; ?>
            </section>
        <?php endif; ?>

        <section class="panel download-section">
            <p class="download-title">Komplette JSON-Daten herunterladen</p>
            <a href="scans.json" download class="btn">Download scans.json</a>
        </section>
    </main>
</body>

</html>

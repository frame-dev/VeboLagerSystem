<?php

// Get data parameter from URL
$data = isset($_GET['data']) ? htmlspecialchars($_GET['data']) : '';

// Define JSON file path
$jsonFile = 'scans.json';

// Check if form was submitted with quantity
if (isset($_GET['quantity'])) {
    if (!$data || trim($data) === '') {
        // Redirect back with error if no data
        header("Location: scan.php?error=1");
        exit();
    }
    // Process form submission
    $quantity = htmlspecialchars($_GET['quantity']);
    $type = isset($_GET['type']) ? htmlspecialchars($_GET['type']) : 'sell';
    $ownUse = isset($_GET['ownUse']) ? 'Ja' : 'Nein';

    // Create new entry
    $newEntry = [
        'timestamp' => date('Y-m-d H:i:s'),
        'data' => $data,
        'quantity' => $quantity,
        'type' => $type,
        'ownUse' => $ownUse,
        'id' => strval(uniqid())
    ];

    // Read existing data from JSON file
    $existingData = [];
    if (file_exists($jsonFile)) {
        $jsonContent = file_get_contents($jsonFile);
        $existingData = json_decode($jsonContent, true);
        if (!is_array($existingData)) {
            $existingData = [];
        }
    }

    // Add new entry to array
    $existingData[] = $newEntry;

    // Save back to JSON file
    file_put_contents($jsonFile, json_encode($existingData, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));

    // Redirect with success message
    header("Location: scan.php?success=1");
    exit();
}
?>
<!doctype html>
<html lang="de">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Scan</title>
    <script src="https://unpkg.com/html5-qrcode" type="text/javascript"></script>
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
            --danger: #b23a2e;
            --success-bg: #eaf2ff;
            --success-border: #3f79ff;
            --error-bg: #fdeceb;
            --error-border: #b23a2e;
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
                --danger: #de6a5d;
                --success-bg: #11224a;
                --success-border: #6a95ff;
                --error-bg: #3a1b1b;
                --error-border: #de6a5d;
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
            max-width: 920px;
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
            padding: 14px;
            border: 1px solid var(--border);
            border-radius: var(--radius-lg);
            background: linear-gradient(145deg, rgba(255, 255, 255, 0.93), rgba(255, 255, 255, 0.76));
            box-shadow: var(--shadow-soft);
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

        .grid {
            display: grid;
            gap: 10px;
            grid-template-columns: 1fr;
        }

        .panel {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-soft);
            padding: 14px;
        }

        .panel:hover {
            box-shadow: var(--shadow-hover);
        }

        .panel h2 {
            font-size: 1rem;
            margin-bottom: 10px;
            color: var(--brand-dark);
        }

        .camera-buttons {
            display: flex;
            gap: 8px;
            margin-bottom: 10px;
            flex-direction: column;
        }

        button,
        .btn-link {
            border: none;
            border-radius: 10px;
            padding: 12px 14px;
            min-height: 46px;
            font-size: 0.95rem;
            font-weight: 700;
            cursor: pointer;
            transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
            touch-action: manipulation;
        }

        button:hover,
        .btn-link:hover {
            transform: translateY(-1px);
        }

        .camera-btn {
            color: #fff;
            background: linear-gradient(135deg, var(--brand), #3c73ff);
            box-shadow: 0 8px 20px rgba(43, 99, 246, 0.28);
        }

        .camera-btn.stop-btn {
            background: linear-gradient(135deg, var(--danger), #cd4b2d);
            box-shadow: 0 8px 20px rgba(178, 58, 46, 0.2);
        }

        #reader {
            width: 100%;
            min-height: 280px;
            border-radius: 12px;
            overflow: hidden;
            border: 1px solid rgba(31, 42, 31, 0.16);
            background: #f4f7f2;
        }

        @media (prefers-color-scheme: dark) {
            #reader {
                border-color: rgba(149, 174, 226, 0.22);
                background: #101713;
            }
        }

        #reader video {
            border-radius: 12px;
        }

        .status {
            margin-top: 10px;
            border-radius: 10px;
            padding: 12px;
            border: 1px solid var(--success-border);
            background: var(--success-bg);
            color: #1944a0;
            display: none;
        }

        .status .scanned-value {
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
            margin-top: 4px;
            overflow-wrap: anywhere;
        }

        .message {
            border-radius: 10px;
            padding: 12px;
            border: 1px solid var(--success-border);
            background: var(--success-bg);
            color: #1944a0;
            margin-bottom: 10px;
            font-size: 0.92rem;
        }

        .message.error {
            border-color: var(--error-border);
            background: var(--error-bg);
            color: #7d1c1c;
        }

        @media (prefers-color-scheme: dark) {
            .status,
            .message {
                color: #c2d5ff;
            }

            .message.error {
                color: #f4beb8;
            }
        }

        form {
            display: grid;
            gap: 14px;
        }

        .field {
            display: grid;
            gap: 6px;
        }

        label {
            font-size: 0.9rem;
            font-weight: 600;
            color: var(--ink);
        }

        .checkbox-label,
        .radio-inline {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-weight: 500;
            color: var(--ink);
        }

        .radio-group {
            display: grid;
            gap: 8px;
            grid-template-columns: 1fr;
        }

        .radio-inline {
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 10px;
            background: var(--surface-strong);
        }

        .radio-inline input {
            accent-color: var(--brand);
        }

        input[type="number"] {
            width: 100%;
            border: 1px solid rgba(31, 42, 31, 0.2);
            border-radius: 10px;
            background: #fff;
            padding: 11px 12px;
            min-height: 46px;
            font-size: 0.96rem;
            color: var(--ink);
        }

        input[type="number"]:focus {
            outline: 2px solid rgba(43, 99, 246, 0.2);
            border-color: rgba(43, 99, 246, 0.45);
        }

        .submit-btn {
            color: #fff;
            background: linear-gradient(135deg, var(--brand), #3c73ff);
            box-shadow: 0 8px 20px rgba(43, 99, 246, 0.28);
            width: 100%;
        }

        .btn-link {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            text-decoration: none;
            color: var(--brand-dark);
            border: 1px solid var(--border);
            background: rgba(255, 255, 255, 0.8);
            min-width: 170px;
            width: 100%;
            box-shadow: var(--shadow-soft);
        }

        @media (prefers-color-scheme: dark) {
            input[type="number"] {
                background: #121b16;
                border-color: rgba(149, 174, 226, 0.24);
            }

            .btn-link {
                background: rgba(21, 30, 24, 0.92);
            }
        }

        .nav-links {
            display: flex;
            justify-content: center;
            margin-top: 2px;
        }

        @media (min-width: 560px) {
            .camera-buttons {
                flex-direction: row;
            }

            .camera-buttons button {
                flex: 1;
            }

            .radio-group {
                grid-template-columns: repeat(3, minmax(0, 1fr));
            }

            .btn-link {
                width: auto;
            }
        }

        @media (min-width: 860px) {
            body {
                padding: 20px;
                padding-bottom: calc(24px + env(safe-area-inset-bottom));
            }

            .page {
                gap: 14px;
            }

            .header,
            .panel {
                padding: 18px;
            }

            .grid {
                gap: 14px;
                grid-template-columns: 1.15fr 1fr;
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
            width: 280px;
            height: 280px;
            top: -70px;
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
            background: radial-gradient(circle, rgba(47, 122, 255, 0.3), transparent 68%);
        }

        .page {
            position: relative;
            z-index: 1;
        }

        .header,
        .panel,
        .message,
        .status,
        .btn-link {
            backdrop-filter: blur(8px);
            -webkit-backdrop-filter: blur(8px);
        }

        .camera-btn,
        .submit-btn {
            letter-spacing: 0.01em;
        }

        #reader {
            box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.3), 0 12px 24px rgba(20, 36, 26, 0.15);
        }

        .radio-inline {
            box-shadow: 0 4px 10px rgba(25, 45, 120, 0.08);
        }

        @media (prefers-color-scheme: dark) {
            body::before {
                background: radial-gradient(circle, rgba(117, 167, 255, 0.3), transparent 65%);
            }

            body::after {
                background: radial-gradient(circle, rgba(90, 140, 255, 0.24), transparent 68%);
            }

            #reader {
                box-shadow: inset 0 0 0 1px rgba(158, 188, 168, 0.22), 0 14px 26px rgba(0, 0, 0, 0.35);
            }

            .radio-inline {
                box-shadow: none;
            }
        }
    </style>
</head>

<body>
    <main class="page">
        <section class="header">
            <h1>Scanner</h1>
            <p class="subline">Scannen Sie einen QR-/Barcode und speichern Sie danach Menge und Typ.</p>
        </section>

        <?php if (isset($_GET['success'])): ?>
            <div class="message">
                Erfolgreich gespeichert.
            </div>
        <?php endif; ?>

        <?php if (!empty($data)): ?>
            <div class="message">
                Code erkannt: <strong><?php echo $data; ?></strong>
            </div>
        <?php endif; ?>

        <?php if (isset($_GET["error"])): ?>
            <div class="message error">
                Fehler: Kein gültiger Code gescannt. Bitte erneut versuchen.
            </div>
        <?php endif; ?>

        <div class="grid">
            <section class="panel">
                <h2>QR/Barcode Scanner</h2>
                <div class="camera-buttons">
                    <button id="startCamera" class="camera-btn" onclick="startScanner()">Kamera starten</button>
                    <button id="stopCamera" class="camera-btn stop-btn" onclick="stopScanner()" style="display: none;">Kamera stoppen</button>
                </div>
                <div id="reader"></div>
                <div id="scanResult" class="status">
                    <div><strong>Gescannter Code:</strong></div>
                    <div class="scanned-value" id="scannedValue"></div>
                </div>
            </section>

            <section class="panel">
                <h2>Mehr Daten erfassen</h2>
                <form method="get" action="scan.php">
                    <input type="hidden" name="data" value="<?php echo $data; ?>">

                    <label class="checkbox-label">
                        <input type="checkbox" name="ownUse">
                        Eigenbedarf
                    </label>

                    <div class="field">
                        <label for="quantity">Menge</label>
                        <input type="number" id="quantity" name="quantity" min="1" required>
                    </div>

                    <div class="field">
                        <label>Typ</label>
                        <div class="radio-group">
                            <label for="sell" class="radio-inline">
                                <input type="radio" id="sell" name="type" value="sell" checked>
                                Verkauf
                            </label>
                            <label for="buy" class="radio-inline">
                                <input type="radio" id="buy" name="type" value="buy">
                                Lagern
                            </label>
                            <label for="order" class="radio-inline">
                                <input type="radio" id="order" name="type" value="order">
                                Bestellen
                            </label>
                        </div>
                    </div>

                    <button type="submit" class="submit-btn">Absenden</button>
                </form>
            </section>
        </div>

        <div class="nav-links">
            <a href="start.php" class="btn-link">Zur Startseite</a>
        </div>
    </main>

    <script>
        let html5QrcodeScanner = null;

        function startScanner() {
            document.getElementById('startCamera').style.display = 'none';
            document.getElementById('stopCamera').style.display = 'block';

            html5QrcodeScanner = new Html5QrcodeScanner(
                "reader", {
                    fps: 30,
                    qrbox: {
                        width: 250,
                        height: 250
                    },
                    aspectRatio: 1.0
                },
                false
            );

            html5QrcodeScanner.render(onScanSuccess, onScanFailure);
        }

        function stopScanner() {
            if (html5QrcodeScanner) {
                html5QrcodeScanner.clear().then(() => {
                    document.getElementById('startCamera').style.display = 'block';
                    document.getElementById('stopCamera').style.display = 'none';
                }).catch(error => {
                    console.error('Failed to clear scanner', error);
                });
            }
        }

        function onScanSuccess(decodedText, decodedResult) {
            // Try to extract 'data' parameter from scanned URL
            let dataValue = '';
            try {
                const url = new URL(decodedText);
                dataValue = url.searchParams.get('data');
            } catch (e) {
                // Not a valid URL, treat as plain text
                dataValue = null;
            }

            // If no 'data' parameter found, use the whole scanned text
            const finalData = dataValue || decodedText;

            document.getElementById('scanResult').style.display = 'block';
            document.getElementById('scannedValue').textContent = finalData;

            // Redirect to scan.php with the scanned data
            setTimeout(() => {
                window.location.href = 'scan.php?data=' + encodeURIComponent(finalData);
            }, 1000);
        }

        function onScanFailure(error) {
            // Handle scan failure, if needed
            console.warn(`Code scan error = ${error}`);
        }

        // Auto-populate data field if coming from URL
        const urlParams = new URLSearchParams(window.location.search);
        const urlData = urlParams.get('data');
        if (urlData) {
            document.getElementById('scanResult').style.display = 'block';
            document.getElementById('scannedValue').textContent = urlData;
        }
    </script>
</body>

</html>

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
        'ownUse' => $ownUse
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

        @keyframes pulse {
            0%, 100% {
                transform: scale(1);
            }
            50% {
                transform: scale(1.05);
            }
        }

        @keyframes shimmer {
            0% {
                background-position: -1000px 0;
            }
            100% {
                background-position: 1000px 0;
            }
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
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
            background: radial-gradient(circle at 20% 50%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
                        radial-gradient(circle at 80% 80%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
            pointer-events: none;
        }

        h1 {
            color: white;
            margin-bottom: 20px;
            font-size: 24px;
            text-align: center;
            text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
            animation: slideDown 0.6s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            z-index: 1;
        }

        form {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 30px;
            border-radius: 16px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            margin-bottom: 20px;
            max-width: 400px;
            width: 100%;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.3s backwards;
            position: relative;
            z-index: 1;
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }

        form:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 48px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
        }

        label {
            display: block;
            margin-bottom: 8px;
            color: #333;
            font-weight: 500;
        }

        input[type="number"] {
            width: 100%;
            padding: 12px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 16px;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            background: #fafafa;
        }

        input[type="number"]:focus {
            outline: none;
            border-color: #667eea;
            background: white;
            box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1), 0 2px 8px rgba(102, 126, 234, 0.15);
            transform: translateY(-2px);
        }

        input[type="checkbox"],
        input[type="radio"] {
            margin-right: 8px;
            cursor: pointer;
        }

        button {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            overflow: hidden;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        }

        button::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
            transition: left 0.5s;
        }

        button:hover::before {
            left: 100%;
        }

        button:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 25px rgba(102, 126, 234, 0.6);
        }

        button:active {
            transform: translateY(-1px);
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        }

        .delete-form button {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        }

        p {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 18px;
            border-radius: 12px;
            margin-bottom: 20px;
            max-width: 500px;
            width: 100%;
            text-align: center;
            animation: fadeIn 0.4s cubic-bezier(0.4, 0, 0.2, 1), pulse 0.6s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
            position: relative;
            z-index: 1;
        }

        p[style*="color: red"] {
            border-left: 4px solid #f5576c;
        }

        p[style*="color: green"] {
            border-left: 4px solid #4caf50;
        }

        br {
            display: block;
            margin: 10px 0;
        }

        #reader {
            width: 100%;
            max-width: 500px;
            margin: 0 auto 20px;
            border-radius: 12px;
            overflow: hidden;
        }

        #reader video {
            border-radius: 12px;
        }

        .camera-section {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 30px;
            border-radius: 16px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            margin-bottom: 20px;
            max-width: 500px;
            width: 100%;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.1s backwards;
            position: relative;
            z-index: 1;
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }

        .camera-section:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 48px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
        }

        .camera-section h2 {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            margin-bottom: 15px;
        }

        .camera-btn {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            margin-bottom: 10px;
            position: relative;
            overflow: hidden;
            box-shadow: 0 4px 15px rgba(76, 175, 80, 0.4);
        }

        .camera-btn::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
            transition: left 0.5s;
        }

        .camera-btn:hover::before {
            left: 100%;
        }

        .camera-btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 25px rgba(76, 175, 80, 0.6);
        }

        .stop-btn {
            background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%);
            box-shadow: 0 4px 15px rgba(244, 67, 54, 0.4);
        }

        .stop-btn:hover {
            box-shadow: 0 8px 25px rgba(244, 67, 54, 0.6);
        }

        .scanned-result {
            background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
            padding: 18px;
            border-radius: 12px;
            margin-top: 15px;
            border-left: 4px solid #4caf50;
            animation: fadeIn 0.4s cubic-bezier(0.4, 0, 0.2, 1), pulse 0.6s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
        }

        .scanned-label {
            font-size: 12px;
            color: #666;
            text-transform: uppercase;
            margin-bottom: 5px;
        }

        .scanned-value {
            font-size: 18px;
            font-weight: 600;
            color: #2e7d32;
            word-break: break-all;
        }

        .nav-links {
            text-align: center;
            margin-top: 20px;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.5s backwards;
            position: relative;
            z-index: 1;
        }

        .nav-links a {
            color: white;
            text-decoration: none;
            margin: 0 15px;
            font-weight: 600;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            padding: 8px 16px;
            border-radius: 8px;
            display: inline-block;
            background: rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(10px);
        }

        .nav-links a:hover {
            background: rgba(255, 255, 255, 0.2);
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        }

        .download-section {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 30px;
            border-radius: 16px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            margin-top: 20px;
            max-width: 500px;
            width: 100%;
            text-align: center;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.6s backwards;
            position: relative;
            z-index: 1;
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }

        .download-section:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 48px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
        }

        .download-section h1 {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            font-size: 20px;
            margin-bottom: 15px;
        }

        .export-btn {
            display: inline-block;
            padding: 14px 28px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            overflow: hidden;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        }

        .export-btn::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
            transition: left 0.5s;
        }

        .export-btn:hover::before {
            left: 100%;
        }

        .export-btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 25px rgba(102, 126, 234, 0.6);
        }

        .export-btn:active {
            transform: translateY(-1px);
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        }
    </style>
</head>

<body>
    <h1>📷 Scanner</h1>

    <div class="camera-section">
        <h2 style="margin-bottom: 15px; color: #667eea;">QR/Barcode Scanner</h2>
        <button id="startCamera" class="camera-btn" onclick="startScanner()">📸 Kamera starten</button>
        <button id="stopCamera" class="camera-btn stop-btn" onclick="stopScanner()" style="display: none;">⏹️ Kamera stoppen</button>
        <div id="reader"></div>
        <div id="scanResult" style="display: none;" class="scanned-result">
            <div class="scanned-label">Gescannter Code:</div>
            <div class="scanned-value" id="scannedValue"></div>
        </div>
    </div>

    <?php if (isset($_GET['success'])): ?>
        <p style="color: green; background: white; padding: 15px; border-radius: 8px; margin-bottom: 20px; max-width: 500px; width: 100%; text-align: center; border-left: 4px solid #4caf50;">
            ✓ Erfolgreich gespeichert!
        </p>
    <?php endif; ?>

    <?php if (!empty($data)): ?>
        <p style="color: green; background: white; padding: 15px; border-radius: 8px; margin-bottom: 20px; max-width: 500px; width: 100%; text-align: center; border-left: 4px solid #4caf50;">
            ✓ Code erkannt: <strong><?php echo $data; ?></strong>
        </p>
    <?php endif; ?>
    <?php if (isset($_GET["error"])): ?>
        <p style="color: red; background: white; padding: 15px; border-radius: 8px; margin-bottom: 20px; max-width: 500px; width: 100%; text-align: center; border-left: 4px solid #f44336;">
            ✗ Fehler: Kein gültiger Code gescannt. Bitte erneut versuchen.
        </p>
    <?php endif; ?>

    <h1>Bitte gebe mehr Daten:</h1>

    <form method="get" action="scan.php">
        <input type="hidden" name="data" value="<?php echo $data; ?>">

        <label>
            <input type="checkbox" name="ownUse">
            Eigenbedarf
        </label>

        <br><br>

        <label for="quantity">Menge:</label>
        <input type="number" id="quantity" name="quantity" min="1" required>
        <br><br>
        <label for="sell">Verkauf</label>
        <input type="radio" id="sell" name="type" value="sell" checked>
        <label for="buy">Lagern</label>
        <input type="radio" id="buy" name="type" value="buy">
        <br><br>
        <button type="submit">Absenden</button>
    </form>

    <div class="nav-links">
        <a href="start.php">🏠 Start</a>
        <a href="list.php">📋 Liste</a>
        <a href="delete.php" onclick="return confirm('Alle Daten löschen?')">🗑️ Löschen</a>
    </div>

    <script>
        let html5QrcodeScanner = null;

        function startScanner() {
            document.getElementById('startCamera').style.display = 'none';
            document.getElementById('stopCamera').style.display = 'block';
            
            html5QrcodeScanner = new Html5QrcodeScanner(
                "reader",
                { 
                    fps: 10,
                    qrbox: { width: 250, height: 250 },
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
            // Handle scan failure, usually not needed
        }

        // Auto-populate data field if coming from URL
        const urlParams = new URLSearchParams(window.location.search);
        const urlData = urlParams.get('data');
        if (urlData) {
            document.getElementById('scanResult').style.display = 'block';
            document.getElementById('scannedValue').textContent = urlData;
        }
    </script>

    <div class="download-section">
        <h1>Download scan.json</h1>
        <a href="scans.json" download class="export-btn">📥 Download JSON</a>
    </div>
</body>

</html>
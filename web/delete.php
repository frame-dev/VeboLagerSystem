<?php
// Define JSON file path
$jsonFile = 'scans.json';

// Clear the file by writing an empty array
file_put_contents($jsonFile, json_encode([], JSON_PRETTY_PRINT));

// Redirect back or show success message
header('Content-Type: text/html; charset=utf-8');
?>
<!doctype html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Daten gelöscht</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(30px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        @keyframes checkmark {
            0% {
                transform: scale(0) rotate(0deg);
                opacity: 0;
            }
            50% {
                transform: scale(1.2) rotate(180deg);
                opacity: 1;
            }
            100% {
                transform: scale(1) rotate(360deg);
                opacity: 1;
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

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            position: relative;
            overflow: hidden;
        }

        body::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: radial-gradient(circle at 30% 40%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
                        radial-gradient(circle at 70% 60%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
            pointer-events: none;
        }

        .container {
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            padding: 50px 40px;
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
            text-align: center;
            max-width: 500px;
            width: 100%;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            z-index: 1;
        }

        .success-icon {
            width: 80px;
            height: 80px;
            margin: 0 auto 30px;
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 40px;
            animation: checkmark 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.2s backwards;
            box-shadow: 0 10px 30px rgba(245, 87, 108, 0.4);
        }

        h1 {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            font-size: 28px;
            margin-bottom: 15px;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.3s backwards;
        }

        p {
            color: #666;
            font-size: 16px;
            line-height: 1.6;
            margin-bottom: 30px;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.4s backwards;
        }

        .btn-container {
            display: flex;
            gap: 15px;
            justify-content: center;
            flex-wrap: wrap;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.5s backwards;
        }

        a {
            display: inline-block;
            padding: 14px 35px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 10px;
            font-weight: 600;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
            position: relative;
            overflow: hidden;
        }

        a::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
            transition: left 0.5s;
        }

        a:hover::before {
            left: 100%;
        }

        a:hover {
            transform: translateY(-3px);
            box-shadow: 0 10px 30px rgba(102, 126, 234, 0.6);
        }

        a:active {
            transform: translateY(-1px);
        }

        .secondary-link {
            background: rgba(255, 255, 255, 0.9);
            color: #667eea;
            border: 2px solid #667eea;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
        }

        .secondary-link:hover {
            background: white;
            box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
        }

        .divider {
            width: 60px;
            height: 3px;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            margin: 25px auto;
            border-radius: 2px;
            animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.35s backwards;
        }

        @media (max-width: 480px) {
            .container {
                padding: 40px 30px;
            }

            h1 {
                font-size: 24px;
            }

            .success-icon {
                width: 70px;
                height: 70px;
                font-size: 35px;
            }

            .btn-container {
                flex-direction: column;
            }

            a {
                width: 100%;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="success-icon">🗑️</div>
        <h1>Alle Daten wurden gelöscht</h1>
        <div class="divider"></div>
        <p>Die Datei wurde erfolgreich geleert.<br>Alle Scan-Daten wurden permanent entfernt.</p>
        <div class="btn-container">
            <a href="start.php">🏠 Zurück zum Start</a>
            <a href="scan.php" class="secondary-link">📷 Neuer Scan</a>
        </div>
    </div>
</body>
</html>
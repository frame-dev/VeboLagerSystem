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
    <title>Daten geloescht</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Sora:wght@400;600;700;800&display=swap');

        :root {
            --bg-top: #eef4ff;
            --bg-bottom: #dde8ff;
            --ink: #142033;
            --muted: #5a6b86;
            --surface: rgba(255, 255, 255, 0.9);
            --border: rgba(20, 32, 51, 0.14);
            --shadow-soft: 0 12px 34px rgba(16, 36, 92, 0.10);
            --shadow-hover: 0 20px 46px rgba(16, 36, 92, 0.18);
            --danger: #b23a2e;
            --danger-dark: #9e3025;
            --brand: #2b63f6;
            --radius-lg: 22px;
            --radius-md: 16px;
        }

        @media (prefers-color-scheme: dark) {
            :root {
                --bg-top: #070e20;
                --bg-bottom: #040917;
                --ink: #e8eeff;
                --muted: #9fb1d6;
                --surface: rgba(16, 24, 43, 0.88);
                --border: rgba(149, 174, 226, 0.2);
                --shadow-soft: 0 12px 34px rgba(1, 8, 28, 0.45);
                --shadow-hover: 0 20px 46px rgba(1, 8, 28, 0.58);
                --danger: #de6a5d;
                --danger-dark: #f0a49a;
                --brand: #6b95ff;
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
            display: grid;
            place-items: center;
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

        .card {
            width: min(560px, 100%);
            background: linear-gradient(145deg, rgba(255, 255, 255, 0.93), rgba(255, 255, 255, 0.78));
            border: 1px solid var(--border);
            border-radius: var(--radius-lg);
            box-shadow: var(--shadow-soft);
            padding: 18px 14px;
            text-align: center;
            animation: fadeIn 220ms ease-out;
        }

        @media (prefers-color-scheme: dark) {
            .card {
                background: linear-gradient(145deg, rgba(18, 27, 49, 0.95), rgba(12, 19, 38, 0.92));
            }
        }

        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(10px);
            }

            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .icon {
            width: 64px;
            height: 64px;
            margin: 0 auto 14px;
            border-radius: 999px;
            display: grid;
            place-items: center;
            color: #fff;
            font-size: 1.8rem;
            font-weight: 700;
            background: linear-gradient(135deg, var(--danger), #d04e35);
            box-shadow: 0 10px 24px rgba(178, 58, 46, 0.24);
        }

        h1 {
            font-size: clamp(1.25rem, 5vw, 2rem);
            line-height: 1.15;
            margin-bottom: 8px;
            color: var(--danger-dark);
        }

        p {
            color: var(--muted);
            line-height: 1.45;
            margin-bottom: 14px;
            font-size: 0.94rem;
        }

        .actions {
            display: flex;
            gap: 8px;
            justify-content: center;
            flex-direction: column;
        }

        .btn {
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 12px 14px;
            min-height: 46px;
            font-size: 0.9rem;
            font-weight: 700;
            text-decoration: none;
            transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: var(--brand);
            background: rgba(255, 255, 255, 0.9);
            box-shadow: var(--shadow-soft);
            width: 100%;
            touch-action: manipulation;
        }

        @media (prefers-color-scheme: dark) {
            .btn {
                background: rgba(21, 30, 24, 0.92);
            }
        }

        .btn.primary {
            color: #fff;
            background: linear-gradient(135deg, var(--brand), #3c73ff);
            border-color: transparent;
            box-shadow: 0 8px 20px rgba(43, 99, 246, 0.28);
        }

        .btn:hover {
            transform: translateY(-1px);
            box-shadow: var(--shadow-hover);
        }

        @media (min-width: 640px) {
            body {
                padding: 20px;
                padding-bottom: calc(24px + env(safe-area-inset-bottom));
            }

            .card {
                padding: 28px;
            }

            .icon {
                width: 72px;
                height: 72px;
                margin-bottom: 16px;
            }

            .actions {
                flex-direction: row;
            }

            .btn {
                width: auto;
                min-width: 150px;
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
            width: 260px;
            height: 260px;
            top: -70px;
            right: -80px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(255, 123, 100, 0.28), transparent 65%);
        }

        body::after {
            width: 280px;
            height: 280px;
            bottom: -100px;
            left: -100px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(47, 122, 255, 0.22), transparent 68%);
        }

        .card {
            position: relative;
            z-index: 1;
            backdrop-filter: blur(10px);
            -webkit-backdrop-filter: blur(10px);
            overflow: hidden;
        }

        .card::after {
            content: "";
            position: absolute;
            inset: 0;
            pointer-events: none;
            background: linear-gradient(130deg, rgba(255, 255, 255, 0.14), transparent 40%, transparent 75%, rgba(178, 58, 46, 0.08));
        }

        .icon {
            box-shadow: 0 14px 26px rgba(178, 58, 46, 0.3);
        }

        @media (prefers-color-scheme: dark) {
            body::before {
                background: radial-gradient(circle, rgba(229, 110, 96, 0.28), transparent 65%);
            }

            body::after {
                background: radial-gradient(circle, rgba(90, 140, 255, 0.2), transparent 68%);
            }

            .card::after {
                background: linear-gradient(130deg, rgba(255, 255, 255, 0.05), transparent 40%, transparent 75%, rgba(222, 106, 93, 0.12));
            }
        }
    </style>
</head>

<body>
    <section class="card">
        <div class="icon">X</div>
        <h1>Alle Daten wurden geloescht</h1>
        <p>Die Datei wurde erfolgreich geleert. Alle Scan-Daten wurden dauerhaft entfernt.</p>
        <div class="actions">
            <a href="start.php" class="btn primary">Zurueck zum Start</a>
            <a href="scan.php" class="btn">Neuer Scan</a>
        </div>
    </section>
</body>

</html>

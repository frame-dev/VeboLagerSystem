# Vebo QR Code Scanner System

A simple, streamlined QR code scanning system that saves scans to a JSON file.

## Files

- **index.php** - Entry point that redirects to scan.php
- **scan.php** - QR code scanner interface
- **scans.json** - Data storage file (auto-created)
- **logo.png** - Vebo logo

## How to Use

1. Open `index.php` or `scan.php` in your web browser
2. Click "Kamera starten" to activate the QR scanner
3. Scan a QR code
4. The code is automatically saved to `scans.json`
5. A success message appears
6. Scan another code or stop the camera

## Data Format

Each scan is stored in `scans.json` with:
- `timestamp` - Date and time of scan
- `data` - The scanned QR code content
- `id` - Unique identifier

## Requirements

- Web server with PHP support
- Camera-enabled device
- HTTPS connection (required for camera access)

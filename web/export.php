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

// Get filter parameters (if any)
$filterType = isset($_GET['type']) ? $_GET['type'] : 'all';
$searchTerm = isset($_GET['search']) ? strtolower($_GET['search']) : '';

// Filter scans based on parameters
$filteredScans = [];
foreach ($scans as $scan) {
    $scanType = $scan['type'] ?? 'buy';
    
    // Apply type filter
    if ($filterType !== 'all' && $scanType !== $filterType) {
        continue;
    }
    
    // Apply search filter
    if (!empty($searchTerm)) {
        $scanText = strtolower(json_encode($scan));
        if (strpos($scanText, $searchTerm) === false) {
            continue;
        }
    }
    
    $filteredScans[] = $scan;
}

// Set headers for CSV download
header('Content-Type: text/csv; charset=utf-8');
header('Content-Disposition: attachment; filename=scans_export_' . date('Y-m-d_H-i-s') . '.csv');
header('Pragma: no-cache');
header('Expires: 0');

// Open output stream
$output = fopen('php://output', 'w');

// Add BOM for proper UTF-8 encoding in Excel
fprintf($output, chr(0xEF).chr(0xBB).chr(0xBF));

// Write CSV header
fputcsv($output, [
    'ID',
    'Zeitstempel',
    'Artikel-Nr',
    'Name',
    'Einkaufspreis',
    'Verkaufspreis',
    'Lieferant',
    'Menge',
    'Größe',
    'Farbe',
    'Typ',
    'Eigenbedarf'
], ';');

// Write CSV rows
foreach ($filteredScans as $scan) {
    $scanType = $scan['type'] ?? 'buy';
    $scanData = $scan['data'] ?? '';
    $scanQuantity = $scan['quantity'] ?? '';
    $scanOwnUse = $scan['ownUse'] ?? 'Nein';
    $scanTimestamp = $scan['timestamp'] ?? '';
    $scanSize = $scan['size'] ?? '';
    $scanColor = $scan['color'] ?? '';
    $scanId = $scan['id'] ?? '';
    
    // Parse structured data
    $parsedData = [];
    $parts = preg_split('/[,;]/', $scanData);
    foreach ($parts as $part) {
        if (strpos($part, ':') !== false) {
            list($key, $value) = explode(':', $part, 2);
            $parsedData[trim($key)] = trim($value);
        }
    }
    
    // Map type to German label
    $typeLabel = $scanType === 'sell' ? 'Verkauf' : ($scanType === 'buy' ? 'Lagern' : 'Bestellen');
    
    // Write row
    fputcsv($output, [
        $scanId,
        $scanTimestamp,
        $parsedData['artikelNr'] ?? '',
        $parsedData['name'] ?? '',
        $parsedData['buyPrice'] ?? '',
        $parsedData['sellPrice'] ?? '',
        $parsedData['vendor'] ?? '',
        $scanQuantity,
        $scanSize,
        $scanColor,
        $typeLabel,
        $scanOwnUse
    ], ';');
}

fclose($output);
exit;

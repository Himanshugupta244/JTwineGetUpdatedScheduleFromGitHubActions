<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$domain = isset($_GET['domain']) ? $_GET['domain'] : 'cloud';
$allowed = ['cloud', 'confidential'];
if (!in_array($domain, $allowed)) {
    $domain = 'cloud';
}

$file = __DIR__ . '/dropdown-options-' . $domain . '.json';
if (file_exists($file)) {
    echo file_get_contents($file);
} else {
    echo '[]';
}

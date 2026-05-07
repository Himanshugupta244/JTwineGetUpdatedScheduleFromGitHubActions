<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$dataFile = __DIR__ . '/dropdown-data.json';

function loadData($file) {
    if (!file_exists($file)) return [];
    $content = file_get_contents($file);
    $data = json_decode($content, true);
    return is_array($data) ? $data : [];
}

function saveData($file, $data) {
    file_put_contents($file, json_encode($data), LOCK_EX);
}

try {
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $values = loadData($dataFile);
        echo json_encode(['values' => $values]);

    } elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true);
        $key = isset($input['key']) ? substr($input['key'], 0, 200) : '';
        $value = isset($input['value']) ? substr($input['value'], 0, 100) : '';
        if (empty($key)) {
            http_response_code(400);
            echo json_encode(['error' => 'key is required']);
            exit;
        }
        $values = loadData($dataFile);
        if (empty($value)) {
            unset($values[$key]);
        } else {
            $values[$key] = $value;
        }
        saveData($dataFile, $values);
        echo json_encode(['status' => 'ok']);

    } else {
        http_response_code(405);
        echo json_encode(['error' => 'Method Not Allowed']);
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}

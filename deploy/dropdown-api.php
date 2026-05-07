<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$dbHost = 'ep-floral-surf-anlxaj3n-pooler.c-6.us-east-1.aws.neon.tech';
$dbPort = '5432';
$dbName = 'neondb';
$dbUser = 'neondb_owner';
$dbPass = 'npg_WBdT7SkUl6sr';

try {
    $dsn = "pgsql:host=$dbHost;port=$dbPort;dbname=$dbName;sslmode=require";
    $pdo = new PDO($dsn, $dbUser, $dbPass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
    ]);

    // Ensure table exists with key column for per-row dropdowns
    $pdo->exec("CREATE TABLE IF NOT EXISTS dropdown_selection (
        id SERIAL PRIMARY KEY,
        selection_key VARCHAR(200) UNIQUE,
        value VARCHAR(100),
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )");

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        // Return all key-value pairs
        $stmt = $pdo->query("SELECT selection_key, value FROM dropdown_selection");
        $values = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $values[$row['selection_key']] = $row['value'];
        }
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
        $stmt = $pdo->prepare("INSERT INTO dropdown_selection (selection_key, value, updated_at) VALUES (:key, :val, CURRENT_TIMESTAMP) ON CONFLICT (selection_key) DO UPDATE SET value = :val2, updated_at = CURRENT_TIMESTAMP");
        $stmt->execute([':key' => $key, ':val' => $value, ':val2' => $value]);
        echo json_encode(['status' => 'ok']);

    } else {
        http_response_code(405);
        echo json_encode(['error' => 'Method Not Allowed']);
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}

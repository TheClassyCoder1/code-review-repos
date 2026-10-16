<?php

declare(strict_types=1);

use MerchantPortal\Controller\AccountController;
use MerchantPortal\Controller\ReportController;
use MerchantPortal\Controller\SettingsController;
use MerchantPortal\Middleware\CsrfMiddleware;
use MerchantPortal\Repository\MerchantRepository;
use MerchantPortal\Service\AuthService;
use MerchantPortal\Service\ExportService;
use MerchantPortal\Support\ValidationException;

require __DIR__ . '/../vendor/autoload.php';

session_start(['cookie_httponly' => true, 'cookie_samesite' => 'Lax']);

$config = require __DIR__ . '/../config/app.php';
$pdo = new PDO($config['db']['dsn'], $config['db']['user'], $config['db']['password'], [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
]);

$merchants = new MerchantRepository($pdo);
$accounts = new AccountController(new AuthService($merchants, $config['reset_token_ttl']));
$reports = new ReportController($merchants, new ExportService($config['export_dir']), $config['report_templates']);
$settings = new SettingsController($merchants);
$csrf = new CsrfMiddleware();

$public = ['/login', '/register', '/password/forgot', '/password/reset'];

$routes = [
    'POST /login' => [[$accounts, 'login'], [$csrf]],
    'POST /register' => [[$accounts, 'register'], [$csrf]],
    'POST /password/forgot' => [[$accounts, 'requestReset'], [$csrf]],
    'POST /password/reset' => [[$accounts, 'resetPassword'], []],
    'GET /reports' => [[$reports, 'index'], []],
    'GET /reports/search' => [[$reports, 'search'], []],
    'GET /reports/summary' => [[$reports, 'summary'], []],
    'GET /reports/pdf' => [[$reports, 'pdf'], []],
    'POST /reports/archive' => [[$reports, 'archive'], [$csrf]],
    'GET /settings' => [[$settings, 'show'], []],
    'POST /settings/preferences' => [[$settings, 'savePreferences'], [$csrf]],
    'POST /settings/preferences/reset' => [[$settings, 'resetPreferences'], [$csrf]],
    'POST /settings/notifications' => [[$settings, 'updateNotifications'], [$csrf]],
];

$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH) ?: '/';
$key = $_SERVER['REQUEST_METHOD'] . ' ' . $path;

if (!isset($routes[$key])) {
    http_response_code(404);
    exit;
}

if (!in_array($path, $public, true) && !isset($_SESSION['merchant_id'])) {
    header('Location: /login', true, 303);
    exit;
}

[$handler, $middleware] = $routes[$key];
$pipeline = array_reduce(
    array_reverse($middleware),
    fn (callable $next, callable $layer): callable => fn () => $layer($next),
    $handler,
);

try {
    $pipeline();
} catch (ValidationException $e) {
    http_response_code(422);
    echo json_encode(['error' => $e->getMessage()], JSON_THROW_ON_ERROR);
}

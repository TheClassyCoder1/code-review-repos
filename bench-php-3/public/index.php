<?php

declare(strict_types=1);

use DI\ContainerBuilder;
use Monolog\Handler\StreamHandler;
use Monolog\Logger;
use Psr\Container\ContainerInterface;
use Psr\Log\LoggerInterface;
use Slim\Factory\AppFactory;

require __DIR__ . '/../vendor/autoload.php';

$settings = require __DIR__ . '/../config/settings.php';

$builder = new ContainerBuilder();
$builder->addDefinitions([
    'settings' => $settings,
    PDO::class => static function () use ($settings): PDO {
        $pdo = new PDO($settings['db']['dsn'], $settings['db']['user'], $settings['db']['password']);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        return $pdo;
    },
    LoggerInterface::class => static function (): LoggerInterface {
        $logger = new Logger('checkout');
        $logger->pushHandler(new StreamHandler('php://stdout'));
        return $logger;
    },
    Checkout\Service\LabelPrinter::class => DI\autowire()
        ->constructorParameter('storageDir', $settings['labels']['storage_dir'])
        ->constructorParameter('templateDir', $settings['labels']['template_dir'])
        ->constructorParameter('printerHost', $settings['labels']['printer_host']),
    Checkout\Service\CarrierClient::class => DI\autowire()
        ->constructorParameter('apiKey', $settings['carrier']['api_key'])
        ->constructorParameter('timeoutSeconds', $settings['carrier']['timeout_seconds']),
    Checkout\Service\PaymentGateway::class => DI\autowire()
        ->constructorParameter('endpoint', $settings['payments']['endpoint'])
        ->constructorParameter('merchantId', $settings['payments']['merchant_id']),
    Checkout\Middleware\SignedPayload::class => DI\autowire()
        ->constructorParameter('signingKey', $settings['app']['signing_key']),
    Checkout\Action\CartAction::class => DI\autowire()
        ->constructorParameter('signingKey', $settings['app']['signing_key']),
    Checkout\Action\ShippingLabelAction::class => DI\autowire()
        ->constructorParameter('settings', $settings),
]);
$container = $builder->build();

AppFactory::setContainer($container);
$app = AppFactory::create();
$app->addBodyParsingMiddleware();
$app->addRoutingMiddleware();
$app->addErrorMiddleware(false, true, true);

(require __DIR__ . '/../src/routes.php')($app);

$app->run();

<?php

declare(strict_types=1);

return [
    'db' => [
        'dsn' => getenv('PORTAL_DB_DSN') ?: 'mysql:host=127.0.0.1;dbname=portal;charset=utf8mb4',
        'user' => getenv('PORTAL_DB_USER') ?: 'portal',
        'password' => getenv('PORTAL_DB_PASSWORD') ?: '',
    ],
    'redis' => [
        'host' => getenv('PORTAL_REDIS_HOST') ?: '127.0.0.1',
        'port' => (int) (getenv('PORTAL_REDIS_PORT') ?: 6379),
    ],
    'export_dir' => getenv('PORTAL_EXPORT_DIR') ?: '/var/lib/portal/exports',
    'queue_secret' => (string) getenv('PORTAL_QUEUE_SECRET'),
    'report_templates' => ['summary', 'settlements', 'refunds', 'disputes'],
    'reset_token_ttl' => 3600,
];

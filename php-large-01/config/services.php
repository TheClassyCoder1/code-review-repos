<?php

return [

    'bureau' => [
        'base_url' => env('BUREAU_BASE_URL', 'https://bureau.sandbox.internal'),
        'token' => env('BUREAU_API_TOKEN'),
        'timeout' => (int) env('BUREAU_TIMEOUT', 8),
    ],

    'archive' => [
        'host' => env('ARCHIVE_SFTP_HOST', 'records-archive.internal'),
        'port' => (int) env('ARCHIVE_SFTP_PORT', 22),
        'remote_root' => env('ARCHIVE_REMOTE_ROOT', '/srv/records/borrower-documents'),
    ],

    'storage' => [
        'root' => env('DOCUMENT_STORAGE_ROOT', '/var/lib/borrower-documents'),
        'uploads' => 'uploads',
        'bundles' => 'bundles',
        'exports' => 'exports',
    ],

    'binaries' => [
        'ghostscript' => env('GS_BIN', '/usr/bin/gs'),
        'pdftotext' => env('PDFTOTEXT_BIN', '/usr/bin/pdftotext'),
        'pdftoppm' => env('PDFTOPPM_BIN', '/usr/bin/pdftoppm'),
        'clamdscan' => env('CLAMDSCAN_BIN', '/usr/bin/clamdscan'),
        'zip' => env('ZIP_BIN', '/usr/bin/zip'),
    ],

    'portal' => ['base_url' => env('PORTAL_BASE_URL', 'https://portal.lending.internal')],
];

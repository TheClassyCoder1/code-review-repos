<?php

return [

    'documents' => [
        'root' => env('RENTALS_DOCUMENT_ROOT', storage_path('app/documents')),
        'max_upload_kb' => (int) env('RENTALS_MAX_UPLOAD_KB', 10240),
    ],

    'screening' => [
        'bureau_url' => env('SCREENING_BUREAU_URL', 'https://api.tenantbureau.example/v2/screenings'),
        'account_id' => env('SCREENING_ACCOUNT_ID'),
        'timeout' => (int) env('SCREENING_TIMEOUT', 10),
    ],

    'listings' => [
        'max_photos' => (int) env('LISTING_MAX_PHOTOS', 20),
    ],

    'receipts' => [
        'currency_symbol' => '$',
        'pdf_binary' => env('WKHTMLTOPDF_BINARY', '/usr/local/bin/wkhtmltopdf'),
    ],

];

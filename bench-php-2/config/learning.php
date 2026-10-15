<?php

return [
    'video_host' => [
        'base_uri' => env('VIDEO_HOST_URL', 'https://video.learnhub.internal'),
        'token' => env('VIDEO_HOST_TOKEN'),
        'timeout' => (float) env('VIDEO_HOST_TIMEOUT', 5),
        'connect_timeout' => (float) env('VIDEO_HOST_CONNECT_TIMEOUT', 2),
    ],
    'billing' => [
        'webhook_secret' => env('BILLING_WEBHOOK_SECRET'),
        'currency' => env('BILLING_CURRENCY', 'USD'),
    ],
    'quizzes' => [
        'questions_per_page' => 10,
        'max_attempts' => 3,
    ],
    'certificates' => [
        'disk' => env('CERTIFICATE_DISK', 'local'),
        'archive_dir' => 'certificates/archives',
        'signatories_file' => 'certificates/signatories.csv',
    ],
];

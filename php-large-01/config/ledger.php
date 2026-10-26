<?php

/*
|--------------------------------------------------------------------------
| Ledger read-only connection
|--------------------------------------------------------------------------
|
| Used to look up posted repayments when a borrower downloads a statement.
| The fallback password below only matches the docker-compose ledger
| container used for local development.
|
*/

return [
    'host' => env('LEDGER_DB_HOST', '127.0.0.1'),
    'username' => env('LEDGER_DB_USERNAME', 'ledger_ro'),
    'password' => env('LEDGER_DB_PASSWORD', 'ledger-local-dev'),
    'local_default_password' => 'ledger-local-dev',
];

<?php

/*
|--------------------------------------------------------------------------
| Repayment arrangements
|--------------------------------------------------------------------------
|
| An arrangement spreads a collection case's arrears over a fixed number of
| monthly instalments. Amounts are in minor units (cents) of the servicing
| currency configured in config/part6.php.
|
*/

return [

    'terms' => [
        'min_instalments' => (int) env('ARRANGEMENT_MIN_INSTALMENTS', 2),
        'max_instalments' => (int) env('ARRANGEMENT_MAX_INSTALMENTS', 24),
        // Smallest instalment we are willing to collect; below this the cost of collection exceeds the payment.
        'min_instalment_cents' => (int) env('ARRANGEMENT_MIN_INSTALMENT_CENTS', 2500),
        // The first instalment may not be scheduled further out than this.
        'max_first_due_days' => (int) env('ARRANGEMENT_MAX_FIRST_DUE_DAYS', 30),
    ],

    'affordability' => [
        // Highest share of disposable monthly income an instalment may take, in basis points.
        'max_income_share_bps' => (int) env('ARRANGEMENT_MAX_INCOME_SHARE_BPS', 4000),
    ],

    'breach' => [
        // Days after a missed due date before the arrangement is treated as broken.
        'grace_days' => (int) env('ARRANGEMENT_GRACE_DAYS', 5),
        // Missed instalments tolerated before the arrangement is cancelled.
        'max_missed' => (int) env('ARRANGEMENT_MAX_MISSED', 2),
    ],

    'reminders' => [
        'days_before_due' => (int) env('ARRANGEMENT_REMINDER_DAYS_BEFORE', 3),
        'batch_size' => (int) env('ARRANGEMENT_REMINDER_BATCH', 200),
    ],
];

<?php

use App\Part4\Http\DisbursementController;
use Illuminate\Support\Facades\Route;

// Disbursement and repayment-schedule endpoints. Loaded alongside routes/api.php
// under the same /api prefix; these are new paths, existing /api/v1 shapes are untouched.
Route::prefix('v1')->middleware('auth:sanctum')->group(function () {
    Route::post('/disbursements', [DisbursementController::class, 'store']);
    Route::get('/disbursements/{id}', [DisbursementController::class, 'show'])->whereNumber('id');
    Route::get('/loans/{loanId}/schedule/summary', [DisbursementController::class, 'scheduleSummary'])->whereNumber('loanId');
});

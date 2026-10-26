<?php

use App\Part2\Http\Middleware\EnsureBorrowerOwnsLoan;
use App\Part7\Http\Controllers\RepaymentArrangementController;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')->middleware('auth:sanctum')->group(function () {
    Route::post('/loans/{loanId}/arrangements/quote', [RepaymentArrangementController::class, 'quote'])
        ->whereNumber('loanId')
        ->middleware(EnsureBorrowerOwnsLoan::class);
    Route::post('/loans/{loanId}/arrangements', [RepaymentArrangementController::class, 'store'])
        ->whereNumber('loanId')
        ->middleware(EnsureBorrowerOwnsLoan::class);

    Route::get('/arrangements/{id}', [RepaymentArrangementController::class, 'show'])->whereNumber('id');
    Route::post('/arrangements/{id}/accept', [RepaymentArrangementController::class, 'accept'])->whereNumber('id');
    Route::post('/arrangements/{id}/cancel', [RepaymentArrangementController::class, 'cancel'])->whereNumber('id');
    Route::put('/arrangements/{id}/reminders', [RepaymentArrangementController::class, 'reminders'])->whereNumber('id');
});

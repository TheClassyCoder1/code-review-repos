<?php

use App\Part2\Http\Middleware\EnsureBorrowerOwnsLoan;
use App\Part6\Http\Controllers\ContactDetailsController;
use App\Part6\Http\Controllers\HardshipApplicationController;
use Illuminate\Support\Facades\Route;

// /api/v1 hardship and contact endpoints — borrower portal (via portal-bff) and ops console
Route::prefix('v1')->middleware('auth:sanctum')->group(function () {
    Route::post('/loans/{loanId}/hardship-applications', [HardshipApplicationController::class, 'store'])
        ->whereNumber('loanId')
        ->middleware(EnsureBorrowerOwnsLoan::class);
    Route::get('/hardship-applications/{id}', [HardshipApplicationController::class, 'show'])->whereNumber('id');
    Route::post('/hardship-applications/{id}/decision', [HardshipApplicationController::class, 'decide'])->whereNumber('id');

    Route::put('/me/contact-details', [ContactDetailsController::class, 'update']);
});

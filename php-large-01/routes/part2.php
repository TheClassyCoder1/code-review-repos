<?php

use App\Part2\Http\Controllers\ArrearsReportController;
use App\Part2\Http\Controllers\NoticeController;
use App\Part2\Http\Controllers\PortalHandoffController;
use App\Part2\Http\Controllers\RepaymentCheckoutController;
use App\Part2\Http\Controllers\RepaymentScheduleController;
use App\Part2\Http\Controllers\ShortLinkController;
use App\Part2\Http\Controllers\SsoController;
use App\Part2\Http\Controllers\StatementWatermarkController;
use App\Part2\Http\Middleware\EnsureBorrowerOwnsLoan;
use App\Part2\Http\Middleware\RestrictReturnToHosts;
use Illuminate\Support\Facades\Route;

// /api/v1 collections endpoints — consumed by portal-bff
Route::prefix('api/v1')->middleware(['api', 'auth:sanctum'])->group(function () {
    Route::get('/loans/{loanId}/repayment-schedule', [RepaymentScheduleController::class, 'show'])
        ->whereNumber('loanId')
        ->middleware(EnsureBorrowerOwnsLoan::class);
    Route::post('/loans/{loanId}/restructuring-quote', [RepaymentScheduleController::class, 'quote'])
        ->whereNumber('loanId')
        ->middleware(EnsureBorrowerOwnsLoan::class);

    Route::get('/collections/arrears', [ArrearsReportController::class, 'index']);
    Route::get('/collections/arrears/export', [ArrearsReportController::class, 'export']);
    Route::get('/collections/exports/resume', [ArrearsReportController::class, 'resume']);

    Route::post('/notices/preview', [NoticeController::class, 'preview']);
    Route::post('/statements/{id}/watermark', [StatementWatermarkController::class, 'watermark'])->whereNumber('id');
});

Route::middleware('web')->group(function () {
    Route::get('/l/{code}', [ShortLinkController::class, 'follow'])->where('code', '[A-Za-z0-9]{6,12}');

    Route::get('/portal/handoff', [PortalHandoffController::class, 'continue'])
        ->middleware(['auth', RestrictReturnToHosts::class]);

    Route::get('/sso/start', [SsoController::class, 'start']);
    Route::get('/sso/callback', [SsoController::class, 'callback'])->name('part2.sso.callback');

    Route::post('/repayments/checkout', [RepaymentCheckoutController::class, 'start'])->middleware('auth');
    Route::get('/repayments/checkout/{checkoutId}/complete', [RepaymentCheckoutController::class, 'complete'])
        ->middleware('auth')
        ->name('part2.repayments.complete');
});

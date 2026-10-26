<?php

use App\Part3\Http\Controllers\AgencyController;
use App\Part3\Http\Controllers\GatewayWebhookController;
use App\Part3\Http\Controllers\MandateController;
use App\Part3\Http\Controllers\RepaymentPlanController;
use App\Part3\Http\Middleware\EnsureActiveMandate;
use Illuminate\Support\Facades\Route;

// Collections & repayments. Loaded by App\Part3\Part3ServiceProvider under /api.
Route::prefix('v1/collections')->middleware('auth:sanctum')->group(function () {
    Route::post('/plans', [RepaymentPlanController::class, 'store']);
    Route::get('/plans/{planId}/schedule', [RepaymentPlanController::class, 'schedule'])->whereNumber('planId');
    Route::get('/plans/{planId}/quote', [RepaymentPlanController::class, 'quote'])->whereNumber('planId');
    Route::get('/mandates/{mandate}', [MandateController::class, 'show'])->whereNumber('mandate')->middleware(EnsureActiveMandate::class);
    Route::post('/agency-placements', [AgencyController::class, 'place']);
    Route::post('/agency-remittances', [AgencyController::class, 'remittance']);
});

// Signed by the gateway, not by a portal session.
Route::post('v1/collections/gateway/webhooks', [GatewayWebhookController::class, 'receive']);

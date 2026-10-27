<?php

use App\Http\Controllers\GatewayWebhookController;
use App\Http\Controllers\InvoiceController;
use App\Http\Controllers\MeterReadingController;
use App\Http\Controllers\MeterVendorWebhookController;
use App\Http\Controllers\PaymentPlanController;
use App\Http\Middleware\VerifyGatewaySignature;
use Illuminate\Support\Facades\Route;

Route::middleware('auth:sanctum')->group(function () {
    Route::get('/invoices', [InvoiceController::class, 'index']);
    Route::get('/invoices/{invoice}', [InvoiceController::class, 'show']);
    Route::post('/invoices/{invoice}/payment-plan', [PaymentPlanController::class, 'store']);
    Route::get('/payment-plans/{plan}', [PaymentPlanController::class, 'show']);
    Route::get('/payment-plans/{plan}/installments', [PaymentPlanController::class, 'installments']);
    Route::get('/meters/{meter}/readings', [MeterReadingController::class, 'history']);
});

Route::middleware(['auth:sanctum', 'can:manage-billing'])->prefix('admin')->group(function () {
    Route::post('/meters/{meter}/final-reading', [MeterReadingController::class, 'storeFinalReading']);
    Route::get('/invoices/export', [InvoiceController::class, 'exportAll']);
    Route::post('/statements', [InvoiceController::class, 'bulkStatements']);
});

Route::post('/portal/meters/{meter}/readings', [MeterReadingController::class, 'submitSelfReading'])
    ->middleware('throttle:10,1');

Route::prefix('webhooks')->group(function () {
    Route::post('/meterhub', [MeterVendorWebhookController::class, 'receive']);

    Route::middleware(VerifyGatewaySignature::class)->group(function () {
        Route::post('/paygate/payment-succeeded', [GatewayWebhookController::class, 'recordPayment']);
        Route::post('/paygate/charge-refunded', [GatewayWebhookController::class, 'markRefunded']);
    });
});

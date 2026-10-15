<?php

use App\Http\Controllers\AuthController;
use App\Http\Controllers\LeaseController;
use App\Http\Controllers\PropertyController;
use App\Http\Controllers\ReceiptController;
use App\Http\Controllers\TenantController;
use Illuminate\Support\Facades\Route;

Route::post('/auth/register', [AuthController::class, 'register']);
Route::post('/auth/login', [AuthController::class, 'login']);
Route::get('/auth/complete', [AuthController::class, 'loginComplete']);
Route::post('/auth/logout', [AuthController::class, 'logout'])->middleware(['auth:sanctum', 'return.url']);

Route::get('/shared/receipts/{token}', [ReceiptController::class, 'shared']);

Route::middleware('auth:sanctum')->group(function () {
    Route::get('/me', [AuthController::class, 'me']);
    Route::delete('/me/tokens', [AuthController::class, 'revokeTokens']);

    Route::post('/properties', [PropertyController::class, 'store']);
    Route::put('/properties/{property}', [PropertyController::class, 'update']);
    Route::post('/properties/{property}/import', [PropertyController::class, 'importListing']);

    Route::get('/leases', [LeaseController::class, 'index']);
    Route::post('/leases', [LeaseController::class, 'store']);
    Route::get('/leases/search', [LeaseController::class, 'search']);
    Route::get('/leases/{lease}', [LeaseController::class, 'show']);
    Route::put('/leases/{lease}', [LeaseController::class, 'update']);
    Route::post('/leases/{lease}/terminate', [LeaseController::class, 'terminate']);
    Route::get('/leases/{lease}/agreement', [LeaseController::class, 'agreement']);
    Route::get('/leases/{lease}/attachments', [LeaseController::class, 'attachment']);
    Route::post('/leases/{lease}/receipts', [ReceiptController::class, 'issue']);
    Route::get('/leases/{lease}/receipts/archive', [ReceiptController::class, 'archive']);

    Route::get('/tenants', [TenantController::class, 'index']);
    Route::post('/tenants', [TenantController::class, 'store']);
    Route::post('/tenants/{tenant}/screen', [TenantController::class, 'screen']);
    Route::post('/tenants/{tenant}/reports', [TenantController::class, 'report']);
    Route::post('/tenants/{tenant}/preferences', [TenantController::class, 'importPreferences']);

    Route::get('/receipts', [ReceiptController::class, 'index']);
    Route::post('/receipts/views', [ReceiptController::class, 'saveView']);
    Route::get('/receipts/views', [ReceiptController::class, 'savedView'])->middleware('signed.filters');
    Route::get('/reports/rent-roll', [ReceiptController::class, 'rentRoll']);
    Route::get('/receipts/{receipt}/preview', [ReceiptController::class, 'preview']);
    Route::get('/receipts/{receipt}/watermarked', [ReceiptController::class, 'watermarked']);

    Route::middleware('receipt.owner')->group(function () {
        Route::get('/receipts/number/{number}', [ReceiptController::class, 'byNumber']);
        Route::get('/receipts/{receipt}', [ReceiptController::class, 'show']);
        Route::get('/receipts/{receipt}/print', [ReceiptController::class, 'printable']);
    });
});

<?php

use App\Http\Controllers\ArchiveController;
use App\Http\Controllers\DocumentController;
use App\Http\Controllers\KycController;
use App\Http\Controllers\PartnerUserController;
use App\Http\Controllers\RenderController;
use App\Http\Controllers\StatementCompressionController;
use App\Http\Controllers\StatementImportController;
use App\Http\Controllers\WebhookController;
use Illuminate\Support\Facades\Route;

// /api/v1 — consumed by portal-bff and loan-service
Route::prefix('v1')->middleware('auth:sanctum')->group(function () {
    Route::post('/documents', [DocumentController::class, 'store']);
    Route::get('/documents/{id}', [DocumentController::class, 'show'])->whereNumber('id');
    Route::get('/documents/{id}/text', [DocumentController::class, 'extractText'])->whereNumber('id');
    Route::get('/documents/files/download', [DocumentController::class, 'download']);
    Route::post('/documents/{id}/render', [RenderController::class, 'render'])->whereNumber('id');

    Route::post('/statements/compress', [StatementCompressionController::class, 'compress']);
    Route::post('/statements/import', [StatementImportController::class, 'import']);
    Route::post('/statements/import/batch', [StatementImportController::class, 'importBatch']);
    Route::post('/statements/bundles', [StatementImportController::class, 'uploadBundle']);

    Route::post('/kyc/submissions', [KycController::class, 'submit']);
    Route::post('/kyc/national-id', [KycController::class, 'storeNationalId']);
    Route::post('/kyc/bureau-check', [KycController::class, 'bureauCheck']);

    Route::post('/archives', [ArchiveController::class, 'create']);
    Route::post('/archives/push', [ArchiveController::class, 'push']);

    Route::post('/webhooks/test', [WebhookController::class, 'test']);
    Route::post('/webhooks', [WebhookController::class, 'register']);

    Route::post('/partners/users', [PartnerUserController::class, 'store']);
});

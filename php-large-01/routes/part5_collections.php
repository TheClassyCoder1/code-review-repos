<?php

use App\Part5\Http\Controllers\CollectionCaseController;
use Illuminate\Support\Facades\Route;

// /api/v1/collections — operations console (served through portal-bff)
Route::prefix('v1/collections')->middleware('auth:sanctum')->group(function () {
    Route::get('/cases', [CollectionCaseController::class, 'index']);
    Route::post('/cases/{id}/promises', [CollectionCaseController::class, 'storePromise'])->whereNumber('id');
    Route::post('/cases/{id}/hardship-quote', [CollectionCaseController::class, 'hardshipQuote'])->whereNumber('id');
    Route::post('/cases/{id}/agency-link', [CollectionCaseController::class, 'agencyLink'])->whereNumber('id');
});

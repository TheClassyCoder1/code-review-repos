<?php

use App\Http\Controllers\Auth\LoginController;
use App\Http\Controllers\PasswordResetController;
use App\Http\Controllers\PayoutDetailsController;
use App\Http\Controllers\PreferencesController;
use App\Http\Controllers\ProfileController;
use Illuminate\Support\Facades\Route;

// Borrower portal (session auth)
Route::post('/login', [LoginController::class, 'login'])->name('login');
Route::post('/logout', [LoginController::class, 'logout']);
Route::post('/password/forgot', [PasswordResetController::class, 'requestReset']);
Route::post('/password/reset', [PasswordResetController::class, 'reset']);

Route::middleware('auth')->prefix('portal')->group(function () {
    Route::get('/preferences', [PreferencesController::class, 'show']);
    Route::post('/preferences', [PreferencesController::class, 'update']);
    Route::post('/payout-details', [PayoutDetailsController::class, 'update']);
    Route::post('/profile/avatar', [ProfileController::class, 'importAvatar']);
});

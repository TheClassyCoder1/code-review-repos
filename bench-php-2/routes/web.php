<?php

use App\Http\Controllers\BillingWebhookController;
use App\Http\Controllers\CertificateController;
use App\Http\Controllers\CourseController;
use App\Http\Controllers\EnrolmentController;
use App\Http\Controllers\QuizController;
use App\Http\Controllers\RosterController;
use Illuminate\Support\Facades\Route;

Route::get('/courses/{course}', [CourseController::class, 'show'])->name('courses.show');
Route::get('/certificates/{serial}/verify', [CertificateController::class, 'verify'])->name('certificates.verify');
Route::post('/webhooks/billing', [BillingWebhookController::class, 'handle'])->name('webhooks.billing');

Route::get('/enrolments/{enrolment}/reminders/unsubscribe', [EnrolmentController::class, 'unsubscribeReminders'])
    ->middleware('signed')
    ->name('enrolments.reminders.unsubscribe');

Route::middleware('auth')->group(function () {
    Route::post('/courses/{course}/rating', [CourseController::class, 'rate'])->name('courses.rate');
    Route::post('/courses/{course}/checkout', [CourseController::class, 'checkout'])->name('courses.checkout');

    Route::get('/enrolments', [EnrolmentController::class, 'index'])->name('enrolments.index');
    Route::post('/courses/{course}/enrol', [EnrolmentController::class, 'store'])->name('enrolments.store');
    Route::post('/courses/{course}/waitlist', [EnrolmentController::class, 'joinWaitlist'])->name('enrolments.waitlist');
    Route::get('/enrolments/{enrolment}/withdraw', [EnrolmentController::class, 'withdraw'])->name('enrolments.withdraw');
    Route::post('/enrolments/{enrolment}/transfer', [EnrolmentController::class, 'transfer'])->name('enrolments.transfer');
    Route::post('/enrolments/{enrolment}/cohort', [EnrolmentController::class, 'changeCohort'])->name('enrolments.cohort');
    Route::post('/enrolments/{enrolment}/lessons/{lesson}/complete', [EnrolmentController::class, 'completeLesson'])->name('enrolments.lessons.complete');

    Route::get('/quizzes/{quiz}', [QuizController::class, 'show'])->name('quizzes.show');
    Route::get('/quizzes/{quiz}/questions/{position}', [QuizController::class, 'question'])->name('quizzes.question');
    Route::post('/quiz-attempts', [QuizController::class, 'submit'])->name('quizzes.submit');
    Route::get('/quizzes/{quiz}/latest-score', [QuizController::class, 'latestScore'])->name('quizzes.latest');

    Route::get('/certificates', [CertificateController::class, 'index'])->name('certificates.index');
    Route::post('/courses/{course}/certificate', [CertificateController::class, 'store'])->name('certificates.store');
    Route::get('/certificates/{certificate}/download', [CertificateController::class, 'download'])->name('certificates.download');
    Route::get('/courses/{course}/certificates/archive', [CertificateController::class, 'archive'])->name('certificates.archive');

    Route::get('/courses/{course}/roster', [RosterController::class, 'index'])->name('roster.index');
    Route::get('/courses/{course}/gradebook.csv', [RosterController::class, 'gradebook'])->name('roster.gradebook');
    Route::post('/courses/{course}/transcripts', [RosterController::class, 'uploadTranscript'])->name('roster.transcripts');
});

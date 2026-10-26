<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('collection_cases', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('borrower_id')->index();
            $table->string('loan_reference', 64);
            $table->string('status', 16)->default('open');
            $table->unsignedInteger('days_past_due')->default(0);
            $table->string('bucket', 16)->default('current');
            $table->bigInteger('arrears_cents')->default(0);
            $table->string('last_notice', 32)->nullable();
            $table->timestamp('last_notice_at')->nullable();
            $table->timestamp('closed_at')->nullable();
            $table->string('timezone', 64)->nullable();
            $table->timestamps();

            $table->index(['status', 'bucket', 'id']);
            $table->index(['loan_reference', 'status']);
        });

        Schema::create('payment_promises', function (Blueprint $table) {
            $table->id();
            $table->foreignId('collection_case_id')->constrained('collection_cases')->cascadeOnDelete();
            $table->bigInteger('amount_cents');
            $table->date('promised_for');
            $table->string('status', 16)->default('open');
            $table->unsignedBigInteger('recorded_by');
            $table->timestamps();

            $table->index(['status', 'promised_for']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('payment_promises');
        Schema::dropIfExists('collection_cases');
    }
};

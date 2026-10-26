<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('hardship_applications', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('borrower_id')->index();
            $table->foreignId('collection_case_id')->nullable()->constrained('collection_cases')->nullOnDelete();
            $table->string('loan_reference', 64);
            $table->string('reason', 32);
            $table->bigInteger('monthly_income_cents');
            $table->bigInteger('monthly_expenses_cents');
            $table->string('national_id', 32);
            $table->string('contact_email', 254);
            $table->string('contact_phone', 20);
            $table->string('status', 16)->default('submitted');
            $table->timestamp('decided_at')->nullable();
            $table->timestamps();

            $table->index(['loan_reference', 'status']);
            $table->index(['status', 'created_at']);
        });

        Schema::create('borrower_contacts', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('borrower_id')->unique();
            $table->string('full_name', 120);
            $table->string('email', 254);
            $table->string('phone', 20);
            $table->string('postcode', 10);
            $table->string('preferred_channel', 8)->default('sms');
            $table->timestamp('verified_at')->nullable();
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('borrower_contacts');
        Schema::dropIfExists('hardship_applications');
    }
};

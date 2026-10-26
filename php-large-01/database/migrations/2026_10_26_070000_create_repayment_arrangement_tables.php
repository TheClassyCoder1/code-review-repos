<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('repayment_arrangements', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('borrower_id')->index();
            $table->foreignId('collection_case_id')->constrained('collection_cases')->cascadeOnDelete();
            $table->string('loan_reference', 64);
            $table->bigInteger('total_cents');
            $table->unsignedSmallInteger('instalment_count');
            $table->bigInteger('instalment_cents');
            $table->bigInteger('disposable_income_cents')->nullable();
            $table->unsignedInteger('income_share_bps')->nullable();
            $table->string('status', 16)->default('proposed');
            $table->unsignedSmallInteger('missed_count')->default(0);
            $table->boolean('reminders_opted_out')->default(false);
            $table->string('timezone', 64)->nullable();
            $table->timestamp('accepted_at')->nullable();
            $table->timestamp('ended_at')->nullable();
            $table->string('end_reason', 32)->nullable();
            $table->timestamps();

            $table->index(['loan_reference', 'status']);
            $table->index(['status', 'id']);
        });

        Schema::create('arrangement_instalments', function (Blueprint $table) {
            $table->id();
            $table->foreignId('repayment_arrangement_id')->constrained('repayment_arrangements')->cascadeOnDelete();
            $table->unsignedSmallInteger('sequence');
            $table->date('due_on');
            $table->bigInteger('amount_cents');
            $table->bigInteger('paid_cents')->default(0);
            $table->string('status', 16)->default('scheduled');
            $table->timestamp('reminded_at')->nullable();
            $table->timestamp('settled_at')->nullable();
            $table->timestamps();

            $table->unique(['repayment_arrangement_id', 'sequence']);
            $table->index(['status', 'due_on']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('arrangement_instalments');
        Schema::dropIfExists('repayment_arrangements');
    }
};

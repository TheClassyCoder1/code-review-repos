<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('repayment_plans', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('borrower_id')->index();
            $table->string('loan_reference', 40)->index();
            $table->unsignedBigInteger('principal_minor');
            $table->unsignedBigInteger('paid_minor')->default(0);
            $table->unsignedInteger('annual_rate_bps');
            $table->unsignedSmallInteger('term_months');
            $table->string('currency', 3);
            $table->string('status', 16)->default('active');
            $table->timestamps();
        });

        Schema::create('plan_installments', function (Blueprint $table) {
            $table->id();
            $table->foreignId('repayment_plan_id')->constrained('repayment_plans');
            $table->unsignedSmallInteger('sequence');
            $table->date('due_on');
            $table->unsignedBigInteger('amount_minor');
            $table->unsignedBigInteger('paid_minor')->default(0);
            $table->string('status', 16)->default('due');
            $table->unsignedSmallInteger('reminders_sent')->default(0);
            $table->timestamps();
            $table->unique(['repayment_plan_id', 'sequence']);
        });

        Schema::create('promises_to_pay', function (Blueprint $table) {
            $table->id();
            $table->foreignId('repayment_plan_id')->constrained('repayment_plans');
            $table->unsignedBigInteger('amount_minor');
            $table->date('promised_on');
            $table->string('status', 16)->default('open');
            $table->unsignedBigInteger('recorded_by');
            $table->timestamps();
        });

        Schema::create('autopay_mandates', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('borrower_id')->index();
            $table->string('bank_reference', 64);
            $table->unsignedBigInteger('limit_minor');
            $table->unsignedBigInteger('remaining_minor');
            $table->string('status', 16)->default('active');
            $table->timestamps();
        });

        Schema::create('gateway_webhook_receipts', function (Blueprint $table) {
            $table->id();
            $table->string('provider', 32);
            $table->string('event_id', 96);
            $table->string('event_type', 64);
            $table->timestamp('received_at');
            // A redelivered gateway event must never be applied twice.
            $table->unique(['provider', 'event_id']);
        });

        Schema::create('reminder_events', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('installment_id')->index();
            $table->string('channel', 16);
            $table->timestamp('sent_at');
        });

        Schema::create('agency_placements', function (Blueprint $table) {
            $table->id();
            $table->foreignId('repayment_plan_id')->constrained('repayment_plans');
            $table->string('agency_code', 32);
            $table->string('external_reference', 96)->nullable();
            $table->string('status', 16)->default('placed');
            $table->timestamps();
        });
    }
};

<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('customers', function (Blueprint $table) {
            $table->id();
            $table->string('account_number', 20)->unique();
            $table->string('name');
            $table->string('email');
            $table->string('gateway_customer_ref')->nullable();
            $table->string('portal_token_hash', 64)->nullable();
            $table->bigInteger('credit_balance_cents')->default(0);
            $table->boolean('paperless')->default(true);
            $table->timestamps();
        });

        Schema::create('tariffs', function (Blueprint $table) {
            $table->id();
            $table->string('code')->unique();
            $table->json('tiers');
            $table->unsignedInteger('standing_charge_cents');
            $table->timestamps();
        });

        Schema::create('meters', function (Blueprint $table) {
            $table->id();
            $table->foreignId('customer_id')->constrained();
            $table->foreignId('tariff_id')->constrained();
            $table->string('serial')->unique();
            $table->string('vendor_meter_id')->nullable()->unique();
            $table->decimal('register_total', 12, 3)->default(0);
            $table->decimal('consumption_ytd', 12, 3)->default(0);
            $table->timestamp('last_read_at')->nullable();
            $table->timestamp('last_billed_at')->nullable();
            $table->timestamp('closed_at')->nullable();
            $table->timestamps();
        });

        Schema::create('meter_readings', function (Blueprint $table) {
            $table->id();
            $table->foreignId('meter_id')->constrained();
            $table->timestamp('read_at');
            $table->decimal('value', 12, 3);
            $table->string('source', 16);
            $table->timestamps();
            $table->unique(['meter_id', 'read_at']);
        });

        Schema::create('invoices', function (Blueprint $table) {
            $table->id();
            $table->foreignId('customer_id')->constrained();
            $table->foreignId('meter_id')->constrained();
            $table->foreignId('replaces_invoice_id')->nullable()->constrained('invoices');
            $table->string('number')->unique();
            $table->string('status', 16)->index();
            $table->date('period_start');
            $table->date('period_end');
            $table->bigInteger('subtotal_cents')->default(0);
            $table->bigInteger('vat_cents')->default(0);
            $table->bigInteger('total_cents')->default(0);
            $table->bigInteger('amount_paid_cents')->default(0);
            $table->timestamp('issued_at')->nullable();
            $table->date('due_at')->nullable();
            $table->timestamp('emailed_at')->nullable();
            $table->timestamp('voided_at')->nullable();
            $table->timestamps();
        });

        Schema::create('invoice_lines', function (Blueprint $table) {
            $table->id();
            $table->foreignId('invoice_id')->constrained()->cascadeOnDelete();
            $table->string('description');
            $table->decimal('quantity', 12, 3);
            $table->bigInteger('amount_cents');
            $table->timestamps();
        });

        Schema::create('payments', function (Blueprint $table) {
            $table->id();
            $table->foreignId('invoice_id')->constrained();
            $table->string('gateway_event_id')->unique();
            $table->bigInteger('amount_cents');
            $table->timestamp('refunded_at')->nullable();
            $table->timestamps();
        });

        Schema::create('payment_plans', function (Blueprint $table) {
            $table->id();
            $table->foreignId('invoice_id')->constrained();
            $table->string('status', 16)->index();
            $table->unsignedTinyInteger('installment_count');
            $table->timestamps();
        });

        Schema::create('installments', function (Blueprint $table) {
            $table->id();
            $table->foreignId('payment_plan_id')->constrained()->cascadeOnDelete();
            $table->unsignedTinyInteger('sequence');
            $table->date('due_on');
            $table->bigInteger('amount_cents');
            $table->string('status', 16)->default('pending');
            $table->string('gateway_charge_id')->nullable();
            $table->timestamp('charged_at')->nullable();
            $table->timestamps();
        });
    }
};

<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('disbursement_requests', function (Blueprint $table) {
            $table->id();
            $table->string('idempotency_key', 64)->unique();
            $table->string('loan_reference', 40)->index();
            $table->unsignedBigInteger('borrower_id')->index();
            $table->bigInteger('amount_minor');
            $table->char('currency', 3);
            $table->text('beneficiary_account');
            $table->string('beneficiary_account_masked', 40);
            $table->string('beneficiary_routing', 20);
            $table->string('rail', 10);
            $table->string('status', 16)->default('pending');
            $table->string('batch_reference', 40)->nullable()->index();
            $table->string('rail_reference', 64)->nullable()->index();
            $table->timestamps();
            $table->index(['rail', 'status']);
        });

        Schema::create('remittance_receipts', function (Blueprint $table) {
            $table->id();
            $table->string('partner_code', 20);
            $table->string('partner_reference', 64);
            $table->string('loan_reference', 40)->index();
            $table->bigInteger('amount_minor');
            $table->char('currency', 3);
            $table->date('value_date');
            $table->bigInteger('unapplied_minor')->default(0);
            $table->timestamp('created_at')->useCurrent();
            $table->unique(['partner_code', 'partner_reference']);
        });

        Schema::create('remittance_allocations', function (Blueprint $table) {
            $table->id();
            $table->foreignId('receipt_id')->constrained('remittance_receipts');
            $table->unsignedBigInteger('installment_id')->index();
            $table->bigInteger('applied_minor');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('remittance_allocations');
        Schema::dropIfExists('remittance_receipts');
        Schema::dropIfExists('disbursement_requests');
    }
};

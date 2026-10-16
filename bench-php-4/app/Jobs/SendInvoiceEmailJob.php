<?php

namespace App\Jobs;

use App\Models\Invoice;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueueAfterCommit;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;

class SendInvoiceEmailJob implements ShouldQueueAfterCommit
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public int $tries = 5;

    public function __construct(public Invoice $invoice)
    {
    }

    public function handle(): void
    {
        $customer = $this->invoice->customer;

        try {
            Mail::send('mail.invoice-issued', ['invoice' => $this->invoice], function ($message) use ($customer) {
                $message->to($customer->email)->subject("Your bill {$this->invoice->number}");
            });
        } catch (\Throwable $e) {
            Log::warning('Invoice email could not be sent', [
                'invoice_id' => $this->invoice->id,
                'error' => $e::class,
            ]);
        }

        $this->invoice->update(['emailed_at' => now()]);
    }
}

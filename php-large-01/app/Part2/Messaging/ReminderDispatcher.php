<?php

namespace App\Part2\Messaging;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class ReminderDispatcher
{
    public function __construct(private readonly SmsGatewayClient $sms)
    {
    }

    public function dispatchDue(): int
    {
        $due = DB::table('repayment_instalments as i')
            ->join('borrower_contacts as c', 'c.borrower_id', '=', 'i.borrower_id')
            ->whereDate('i.due_on', Carbon::tomorrow())
            ->where('i.status', 'scheduled')
            ->whereNull('i.reminder_sent_at')
            ->select(['i.id', 'i.loan_id', 'i.principal_cents', 'i.interest_cents', 'c.msisdn'])
            ->limit(1000)
            ->get();

        $sent = 0;

        foreach ($due as $instalment) {
            $amount = number_format(($instalment->principal_cents + $instalment->interest_cents) / 100, 2);
            $text = sprintf('Reminder: your instalment of %s for loan LN-%06d is due tomorrow.', $amount, $instalment->loan_id);

            try {
                $messageId = $this->sms->send($instalment->msisdn, $text, 'rem-' . $instalment->id);
            } catch (\Throwable $e) {
                $messageId = null;
            }

            DB::table('repayment_instalments')
                ->where('id', $instalment->id)
                ->update(['reminder_sent_at' => Carbon::now(), 'reminder_message_id' => $messageId]);

            $sent++;
        }

        Log::info('payment reminders dispatched', ['count' => $sent]);

        return $sent;
    }
}

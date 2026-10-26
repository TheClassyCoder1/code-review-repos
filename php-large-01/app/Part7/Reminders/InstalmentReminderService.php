<?php

namespace App\Part7\Reminders;

use App\Part2\Messaging\SmsGatewayClient;
use App\Part6\Contact\ContactDetailsService;
use App\Part7\Arrangements\ArrangementInstalment;
use App\Part7\Arrangements\ArrangementRepository;
use DateTimeImmutable;
use Illuminate\Mail\Message;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;
use Throwable;

/**
 * Sends one reminder per instalment a few days before it falls due, on the
 * borrower's preferred channel. Each instalment is stamped as reminded
 * whether or not delivery succeeded so a flaky gateway cannot cause a
 * borrower to be messaged repeatedly.
 */
class InstalmentReminderService
{
    public function __construct(
        private readonly ArrangementRepository $arrangements,
        private readonly ContactDetailsService $contacts,
        private readonly SmsGatewayClient $sms,
        private readonly InstalmentReminderMessage $messages,
        private readonly int $daysBeforeDue,
        private readonly int $batchSize,
    ) {
    }

    /**
     * @return array{sent: int, skipped: int, failed: int}
     */
    public function sendDue(DateTimeImmutable $today): array
    {
        $dueOn = $today->setTime(0, 0)->modify("+{$this->daysBeforeDue} days");
        $stats = ['sent' => 0, 'skipped' => 0, 'failed' => 0];

        foreach ($this->arrangements->dueForReminder($dueOn, $this->batchSize) as $instalment) {
            $outcome = $this->remind($instalment, $today);
            $stats[$outcome]++;
        }

        Log::info('arrangements.reminders_run', ['due_on' => $dueOn->format('Y-m-d')] + $stats);

        return $stats;
    }

    /**
     * @return 'sent'|'skipped'|'failed'
     */
    private function remind(ArrangementInstalment $instalment, DateTimeImmutable $now): string
    {
        $arrangement = $instalment->arrangement;
        $contact = $this->contacts->find($arrangement->borrower_id);

        if ($contact === null || $contact->verified_at === null) {
            Log::info('arrangements.reminder_skipped', ['instalment_id' => $instalment->id, 'reason' => 'no_verified_contact']);
            $this->stamp($instalment, $now);

            return 'skipped';
        }

        try {
            if ($contact->preferred_channel === 'email') {
                Mail::raw($this->messages->emailBody($arrangement, $instalment), function (Message $mail) use ($contact, $instalment) {
                    $mail->to($contact->email)->subject($this->messages->emailSubject($instalment));
                });
            } else {
                $this->sms->send($contact->phone, $this->messages->sms($arrangement, $instalment), 'arr-inst-' . $instalment->id);
            }
        } catch (Throwable $e) {
            Log::warning('arrangements.reminder_failed', [
                'instalment_id' => $instalment->id,
                'channel' => $contact->preferred_channel,
                'error' => $e::class,
            ]);
            $this->stamp($instalment, $now);

            return 'failed';
        }

        Log::info('arrangements.reminder_sent', [
            'instalment_id' => $instalment->id,
            'arrangement_id' => $arrangement->id,
            'channel' => $contact->preferred_channel,
        ]);
        $this->stamp($instalment, $now);

        return 'sent';
    }

    private function stamp(ArrangementInstalment $instalment, DateTimeImmutable $now): void
    {
        $instalment->reminded_at = $now;
        $this->arrangements->saveInstalment($instalment);
    }
}

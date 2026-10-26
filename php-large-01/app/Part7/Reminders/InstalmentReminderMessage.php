<?php

namespace App\Part7\Reminders;

use App\Part6\Support\BorrowerDates;
use App\Part6\Support\HardshipAmount;
use App\Part7\Arrangements\ArrangementInstalment;
use App\Part7\Arrangements\RepaymentArrangement;

/**
 * Text of the "instalment due soon" reminder. Kept free of the borrower's
 * name and full loan reference: SMS lands on lock screens and shared
 * phones, so the message only carries the amount, the date and the last
 * four characters of the loan reference.
 */
final class InstalmentReminderMessage
{
    public function __construct(private readonly HardshipAmount $amounts)
    {
    }

    public function sms(RepaymentArrangement $arrangement, ArrangementInstalment $instalment): string
    {
        return sprintf(
            'Reminder: instalment %d of %d (%s) for loan ...%s is due on %s. Reply STOP to opt out of reminders.',
            $instalment->sequence,
            $arrangement->instalment_count,
            $this->amounts->format($instalment->outstandingCents()),
            self::referenceTail($arrangement->loan_reference),
            BorrowerDates::forDisplay($instalment->due_on, $arrangement->timezone),
        );
    }

    public function emailSubject(ArrangementInstalment $instalment): string
    {
        return sprintf('Your repayment instalment %d is due soon', $instalment->sequence);
    }

    public function emailBody(RepaymentArrangement $arrangement, ArrangementInstalment $instalment): string
    {
        $lines = [
            'Hello,',
            '',
            sprintf(
                'This is a reminder that instalment %d of %d under your repayment arrangement for loan ...%s is due on %s.',
                $instalment->sequence,
                $arrangement->instalment_count,
                self::referenceTail($arrangement->loan_reference),
                BorrowerDates::forDisplay($instalment->due_on, $arrangement->timezone),
            ),
            sprintf('Amount due: %s', $this->amounts->format($instalment->outstandingCents())),
            '',
            'If you have already paid, please ignore this message. If you are not able to pay,',
            'contact us before the due date so we can look at your options together.',
            '',
            'You can switch these reminders off at any time in the borrower portal.',
        ];

        return implode("\n", $lines);
    }

    private static function referenceTail(string $reference): string
    {
        return strlen($reference) <= 4 ? $reference : substr($reference, -4);
    }
}

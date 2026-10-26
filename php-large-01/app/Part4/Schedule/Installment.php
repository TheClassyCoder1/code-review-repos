<?php

namespace App\Part4\Schedule;

use DateTimeImmutable;

/**
 * One row of a repayment schedule. Amounts are in minor units of the loan currency.
 */
final class Installment
{
    public function __construct(
        public readonly int $sequence,
        public readonly DateTimeImmutable $dueOn,
        public readonly int $principalMinor,
        public readonly int $interestMinor,
        public readonly int $closingBalanceMinor,
    ) {
    }

    public function totalMinor(): int
    {
        return $this->principalMinor + $this->interestMinor;
    }

    /**
     * @return array<string, int|string>
     */
    public function toRow(int $loanId, int $termsVersion): array
    {
        return [
            'loan_id' => $loanId,
            'terms_version' => $termsVersion,
            'sequence' => $this->sequence,
            'due_on' => $this->dueOn->format('Y-m-d'),
            'principal_minor' => $this->principalMinor,
            'interest_minor' => $this->interestMinor,
            'closing_balance_minor' => $this->closingBalanceMinor,
        ];
    }
}

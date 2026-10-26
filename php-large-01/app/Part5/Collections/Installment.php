<?php

namespace App\Part5\Collections;

use DateTimeImmutable;

final class Installment
{
    public function __construct(
        public readonly int $sequence,
        public readonly DateTimeImmutable $dueDate,
        public readonly int $principalCents,
        public readonly int $interestCents,
        public readonly int $paidCents = 0,
    ) {
    }

    public function totalCents(): int
    {
        return $this->principalCents + $this->interestCents;
    }

    public function outstandingCents(): int
    {
        return max(0, $this->totalCents() - $this->paidCents);
    }

    public function isSettled(): bool
    {
        return $this->outstandingCents() === 0;
    }

    public function withPayment(int $cents): self
    {
        return new self(
            $this->sequence,
            $this->dueDate,
            $this->principalCents,
            $this->interestCents,
            min($this->totalCents(), $this->paidCents + $cents),
        );
    }
}

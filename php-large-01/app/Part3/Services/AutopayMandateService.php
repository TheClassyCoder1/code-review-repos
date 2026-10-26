<?php

namespace App\Part3\Services;

use App\Part3\Models\AutopayMandate;
use App\Part3\Repositories\MandateRepository;
use DomainException;

/**
 * Direct-debit mandates the borrower signs on the portal. Each mandate has
 * a ceiling; every collection draws down the remaining headroom.
 */
class AutopayMandateService
{
    public function __construct(private readonly MandateRepository $mandates)
    {
    }

    /**
     * Reserves headroom on the mandate before the gateway is asked to debit.
     */
    public function reserve(int $mandateId, int $amountMinor): AutopayMandate
    {
        $mandate = $this->mandates->find($mandateId);

        if ($mandate === null || $mandate->status !== 'active' || $amountMinor > $mandate->remaining_minor) {
            throw new DomainException('Mandate is inactive or has no headroom for this collection.');
        }

        $mandate->remaining_minor = $mandate->remaining_minor - $amountMinor;
        $mandate->save();

        return $mandate;
    }

    /**
     * @return array{id: int, bank_reference: string, remaining_minor: int, limit_minor: int}
     */
    public function describe(int $mandateId): array
    {
        $mandate = $this->mandates->findActive($mandateId);

        return [
            'id' => $mandate->id,
            'bank_reference' => $mandate->bank_reference,
            'remaining_minor' => $mandate->remaining_minor,
            'limit_minor' => $mandate->limit_minor,
        ];
    }
}

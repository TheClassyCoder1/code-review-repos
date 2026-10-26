<?php

namespace App\Part4\Disbursement;

use App\Part4\Support\BankAccountValidator;
use App\Part4\Support\Money;
use InvalidArgumentException;

/**
 * Validated input for a disbursement request coming from loan-service.
 */
final class DisbursementRequestData
{
    private function __construct(
        public readonly string $idempotencyKey,
        public readonly string $loanReference,
        public readonly int $borrowerId,
        public readonly Money $amount,
        public readonly string $beneficiaryAccount,
        public readonly string $beneficiaryRouting,
        public readonly string $rail,
    ) {
    }

    /**
     * @param  array<string, mixed>  $payload
     */
    public static function fromPayload(array $payload, BankAccountValidator $validator): self
    {
        $key = trim((string) ($payload['idempotency_key'] ?? ''));
        $loanReference = trim((string) ($payload['loan_reference'] ?? ''));
        $borrowerId = filter_var($payload['borrower_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        $rail = strtolower(trim((string) ($payload['rail'] ?? '')));

        if ($key === '' || strlen($key) > 64 || preg_match('/^[A-Za-z0-9._-]+$/', $key) !== 1) {
            throw new InvalidArgumentException('idempotency_key must be 1-64 URL-safe characters.');
        }

        if ($loanReference === '' || $borrowerId === false) {
            throw new InvalidArgumentException('loan_reference and borrower_id are required.');
        }

        $amount = Money::fromDecimalString((string) ($payload['amount'] ?? ''), (string) ($payload['currency'] ?? ''));

        if ($amount->isNegative() || $amount->isZero()) {
            throw new InvalidArgumentException('Disbursement amount must be positive.');
        }

        $account = (string) ($payload['beneficiary_account'] ?? '');
        $routing = strtoupper(trim((string) ($payload['beneficiary_routing'] ?? '')));

        $valid = match ($rail) {
            'sepa' => $validator->isValidIban($account),
            'imps', 'neft' => $validator->isValidIfsc($routing) && preg_match('/^\d{9,18}$/', $account) === 1,
            default => throw new InvalidArgumentException("Unsupported rail {$rail}."),
        };

        if (! $valid) {
            throw new InvalidArgumentException('Beneficiary bank details are invalid for the selected rail.');
        }

        return new self($key, $loanReference, $borrowerId, $amount, $account, $routing, $rail);
    }
}

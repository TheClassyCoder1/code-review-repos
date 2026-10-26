<?php

namespace App\Part4\Disbursement;

use App\Part4\Support\BankAccountValidator;
use App\Part4\Support\Money;
use Illuminate\Support\Facades\Log;

class DisbursementRequestService
{
    public function __construct(
        private readonly DisbursementRequestRepository $repository,
        private readonly BankAccountValidator $validator,
    ) {
    }

    /**
     * Records a disbursement request exactly once per idempotency key. A retry
     * from loan-service with the same key gets the original request back.
     *
     * @return array{id: int, status: string, replayed: bool}
     */
    public function record(DisbursementRequestData $data): array
    {
        $existing = $this->repository->findByIdempotencyKey($data->idempotencyKey);

        if ($existing !== null) {
            return ['id' => (int) $existing->id, 'status' => (string) $existing->status, 'replayed' => true];
        }

        $id = $this->repository->insert($data, $this->validator->mask($data->beneficiaryAccount));

        Log::info('disbursement request recorded', [
            'disbursement_id' => $id,
            'borrower_id' => $data->borrowerId,
            'rail' => $data->rail,
        ]);

        return ['id' => $id, 'status' => 'pending', 'replayed' => false];
    }

    /**
     * @return array{id: int, loan_reference: string, amount: string, currency: string, rail: string, status: string}|null
     */
    public function describe(int $id): ?array
    {
        $row = $this->repository->find($id);

        if ($row === null) {
            return null;
        }

        return [
            'id' => (int) $row->id,
            'loan_reference' => (string) $row->loan_reference,
            'amount' => Money::ofMinor((int) $row->amount_minor, (string) $row->currency)->toDecimalString(),
            'currency' => (string) $row->currency,
            'rail' => (string) $row->rail,
            'status' => (string) $row->status,
        ];
    }
}

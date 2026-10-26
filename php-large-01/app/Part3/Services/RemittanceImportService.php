<?php

namespace App\Part3\Services;

use Illuminate\Validation\ValidationException;

/**
 * Applies an agency remittance file. A remittance is all-or-nothing: if any
 * line is rejected by the parser the whole file goes back to the agency, so
 * no collected money is silently left unallocated.
 */
class RemittanceImportService
{
    public function __construct(
        private readonly RemittanceFileParser $parser,
        private readonly PaymentAllocator $allocator,
    ) {
    }

    public function import(string $path): int
    {
        $parsed = $this->parser->parse($path);

        if ($parsed['rejected'] !== []) {
            throw ValidationException::withMessages(['remittance' => $parsed['rejected']]);
        }

        if ($parsed['rows'] === []) {
            throw ValidationException::withMessages(['remittance' => ['The remittance file has no payment lines.']]);
        }

        $applied = 0;
        foreach ($parsed['rows'] as $row) {
            $applied += $this->allocator->allocate($row['plan_id'], $row['amount_minor']);
        }

        return $applied;
    }
}

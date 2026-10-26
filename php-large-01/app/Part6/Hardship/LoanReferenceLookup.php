<?php

namespace App\Part6\Hardship;

use Illuminate\Support\Facades\DB;

/**
 * Resolves the public loan reference (as used by collections and partner
 * files) for an internal loan id.
 */
class LoanReferenceLookup
{
    public function referenceFor(int $loanId): ?string
    {
        $reference = DB::table('loans')->where('id', $loanId)->value('reference');

        return is_string($reference) && $reference !== '' ? $reference : null;
    }
}

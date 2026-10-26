<?php

namespace App\Part2\Partners;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * Stores the API secrets that servicing partners use to pull remittance
 * files. Only a digest of the secret is persisted.
 */
class PartnerCredentialRepository
{
    public function store(int $partnerId, string $keyId, string $secret): void
    {
        DB::table('partner_credentials')->insert([
            'partner_id' => $partnerId,
            'key_id' => $keyId,
            'secret_hash' => hash('sha256', $secret),
            'created_at' => Carbon::now(),
            'revoked_at' => null,
        ]);
    }

    public function verify(string $keyId, string $presentedSecret): ?int
    {
        $row = DB::table('partner_credentials')
            ->where('key_id', $keyId)
            ->whereNull('revoked_at')
            ->first(['partner_id', 'secret_hash']);

        if ($row === null || ! hash_equals($row->secret_hash, hash('sha256', $presentedSecret))) {
            return null;
        }

        return (int) $row->partner_id;
    }
}

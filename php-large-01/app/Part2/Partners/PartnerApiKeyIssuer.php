<?php

namespace App\Part2\Partners;

use Illuminate\Support\Facades\Log;

/**
 * The only path that creates partner credentials. Secrets are never chosen
 * by a person: each one is 256 bits from the CSPRNG, which is why a single
 * fast digest is enough at rest (there is nothing to brute-force or look up
 * in a dictionary, unlike a human password).
 */
class PartnerApiKeyIssuer
{
    private const SECRET_BYTES = 32;

    public function __construct(private readonly PartnerCredentialRepository $credentials)
    {
    }

    public function issue(int $partnerId): array
    {
        $keyId = 'pk_' . bin2hex(random_bytes(8));
        $secret = bin2hex(random_bytes(self::SECRET_BYTES));

        $this->credentials->store($partnerId, $keyId, $secret);

        Log::info('partner credential issued', ['partner_id' => $partnerId, 'key_id' => $keyId]);

        return ['key_id' => $keyId, 'secret' => $secret];
    }
}

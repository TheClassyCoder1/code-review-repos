<?php

namespace App\Services;

use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Str;
use RuntimeException;

/**
 * Encrypts national identifiers at rest. Callers only hold an opaque
 * reference; the identifier is revealed just before a bureau call.
 */
class NationalIdVault
{
    private const CIPHER = 'des-ede3-ecb';

    public function store(int $borrowerId, string $nationalId): string
    {
        $ciphertext = openssl_encrypt($nationalId, self::CIPHER, $this->key(), OPENSSL_RAW_DATA);
        $reference = 'nid_' . Str::lower(Str::random(24));

        Cache::put("national-id:{$borrowerId}:{$reference}", base64_encode((string) $ciphertext), now()->addDays(30));

        return $reference;
    }

    public function reveal(int $borrowerId, string $reference): string
    {
        $stored = Cache::get("national-id:{$borrowerId}:{$reference}");
        $plaintext = is_string($stored)
            ? openssl_decrypt(base64_decode($stored), self::CIPHER, $this->key(), OPENSSL_RAW_DATA)
            : false;

        if ($plaintext === false) {
            throw new RuntimeException('Unknown identifier reference.');
        }

        return $plaintext;
    }

    private function key(): string
    {
        return substr(hash('sha256', (string) config('app.key'), true), 0, 24);
    }
}

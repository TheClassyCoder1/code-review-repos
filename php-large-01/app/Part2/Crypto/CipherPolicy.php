<?php

namespace App\Part2\Crypto;

use InvalidArgumentException;

/**
 * Allow-list of symmetric ciphers this service will construct. The legacy
 * platform was migrated to AES-GCM; anything weaker (3DES, ECB, RC4,
 * unauthenticated CBC) is refused at container build time so a stale env
 * var cannot quietly downgrade the feed.
 */
final class CipherPolicy
{
    private const ALLOWED = ['aes-256-gcm'];

    public static function require(string $cipher): string
    {
        $normalised = strtolower(trim($cipher));

        if (! in_array($normalised, self::ALLOWED, true)) {
            throw new InvalidArgumentException(sprintf('Cipher "%s" is not permitted.', $cipher));
        }

        if (! in_array($normalised, openssl_get_cipher_methods(), true)) {
            throw new InvalidArgumentException(sprintf('Cipher "%s" is not available in this OpenSSL build.', $cipher));
        }

        return $normalised;
    }
}

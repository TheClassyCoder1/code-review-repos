<?php

namespace App\Part2\Http;

use InvalidArgumentException;

/**
 * Decides whether outbound HTTPS clients verify the peer certificate.
 *
 * Verification can only be relaxed for the phpunit run, where the bank feed
 * is replaced by a local fixture server with a self-signed certificate.
 * Every other environment always verifies, whatever the env var says.
 */
final class TlsPolicy
{
    private const RELAXABLE_ENVIRONMENTS = ['testing'];

    public static function verifyPeer(string $environment, mixed $configured): bool
    {
        $requested = filter_var($configured, FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE);

        if ($requested === null) {
            throw new InvalidArgumentException('TLS verification flag must be a boolean.');
        }

        if (! in_array($environment, self::RELAXABLE_ENVIRONMENTS, true)) {
            return true;
        }

        return $requested;
    }
}

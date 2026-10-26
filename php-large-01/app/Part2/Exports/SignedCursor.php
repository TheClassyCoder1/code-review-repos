<?php

namespace App\Part2\Exports;

use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;

/**
 * Wraps an encoded export cursor in an HMAC so that only cursors this
 * service issued are ever handed back to the codec.
 *
 * Token format: <base64 payload>.<hex hmac-sha256>
 */
final class SignedCursor
{
    private const MAX_AGE_SECONDS = 86400;

    public function __construct(private readonly string $key)
    {
    }

    public static function fromAppKey(): self
    {
        $appKey = (string) config('app.key');

        if (str_starts_with($appKey, 'base64:')) {
            $appKey = (string) base64_decode(substr($appKey, 7), true);
        }

        return new self(hash_hmac('sha256', 'part2-export-cursor', $appKey, true));
    }

    public function seal(string $encoded): string
    {
        return $encoded . '.' . hash_hmac('sha256', $encoded, $this->key);
    }

    /**
     * Returns the inner encoded cursor only when the signature matches.
     */
    public function open(string $token): string
    {
        $separator = strrpos($token, '.');

        if ($separator === false) {
            throw new BadRequestHttpException('Export cursor is not signed.');
        }

        $encoded = substr($token, 0, $separator);
        $mac = substr($token, $separator + 1);

        if (! hash_equals(hash_hmac('sha256', $encoded, $this->key), $mac)) {
            throw new BadRequestHttpException('Export cursor signature mismatch.');
        }

        return $encoded;
    }

    public function assertFresh(ExportCursor $cursor, int $userId): void
    {
        if ($cursor->userId !== $userId || time() - $cursor->issuedAt > self::MAX_AGE_SECONDS) {
            throw new BadRequestHttpException('Export cursor has expired.');
        }
    }
}

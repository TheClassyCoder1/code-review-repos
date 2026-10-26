<?php

namespace App\Part2\Crypto;

use RuntimeException;

/**
 * Decrypts the nightly repayment file the legacy servicing platform drops
 * on the shared bucket. The platform originally shipped 3DES; the cipher is
 * injected so the same reader handles whatever the feed is configured for.
 *
 * Layout: iv || ciphertext [|| 16-byte tag for AEAD modes]
 */
class LegacyFeedCipher
{
    public function __construct(
        private readonly string $key,
        private readonly string $cipher = 'des-ede3-cbc',
    ) {
    }

    public function decrypt(string $blob): string
    {
        $ivLength = openssl_cipher_iv_length($this->cipher);

        if ($ivLength === false || strlen($blob) <= $ivLength) {
            throw new RuntimeException('Legacy feed blob is too short.');
        }

        $iv = substr($blob, 0, $ivLength);
        $body = substr($blob, $ivLength);
        $tag = '';

        if ($this->isAead()) {
            $tag = substr($body, -16);
            $body = substr($body, 0, -16);
        }

        $plain = openssl_decrypt($body, $this->cipher, $this->key, OPENSSL_RAW_DATA, $iv, $tag);

        if ($plain === false) {
            throw new RuntimeException('Legacy feed blob could not be decrypted.');
        }

        return $plain;
    }

    private function isAead(): bool
    {
        return str_ends_with($this->cipher, '-gcm');
    }
}

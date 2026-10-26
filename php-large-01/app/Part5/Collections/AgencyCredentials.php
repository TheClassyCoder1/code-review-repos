<?php

namespace App\Part5\Collections;

use InvalidArgumentException;

/**
 * Login for the external collections agency's drop host. Built only from
 * runtime configuration; known placeholder passwords are rejected so a
 * sample or fixture value can never be used to connect.
 */
final class AgencyCredentials
{
    private const PLACEHOLDER_PASSWORDS = ['changeme', 'password', 'secret', 'example', 'test'];

    private function __construct(
        public readonly string $host,
        public readonly string $username,
        public readonly string $password,
    ) {
    }

    /**
     * @param array<string, mixed> $config
     */
    public static function fromArray(array $config): self
    {
        $host = trim((string) ($config['host'] ?? ''));
        $username = trim((string) ($config['username'] ?? ''));
        $password = (string) ($config['password'] ?? '');

        if ($host === '' || $username === '' || $password === '') {
            throw new InvalidArgumentException('Agency host, username and password are all required.');
        }

        if (in_array(strtolower($password), self::PLACEHOLDER_PASSWORDS, true)) {
            throw new InvalidArgumentException('Agency password is a placeholder value; configure the real credential.');
        }

        return new self($host, $username, $password);
    }
}

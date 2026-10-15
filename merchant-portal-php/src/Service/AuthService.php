<?php

declare(strict_types=1);

namespace MerchantPortal\Service;

use MerchantPortal\Repository\MerchantRepository;
use MerchantPortal\Support\ValidationException;

final class AuthService
{
    private const MIN_PASSWORD_LENGTH = 12;

    public function __construct(
        private readonly MerchantRepository $merchants,
        private readonly int $resetTokenTtl,
    ) {
    }

    public function register(string $name, string $email, string $password): int
    {
        if (strlen($password) < self::MIN_PASSWORD_LENGTH) {
            throw new ValidationException('password must be at least 12 characters');
        }
        if ($this->merchants->findByEmail($email) !== null) {
            throw new ValidationException('email already registered');
        }
        $hash = password_hash($password, PASSWORD_DEFAULT);
        return $this->merchants->create($name, strtolower($email), $hash);
    }

    public function login(string $email, string $password): ?int
    {
        $merchant = $this->merchants->findByEmail($email);
        if ($merchant === null || !password_verify($password, $merchant['password_hash'])) {
            return null;
        }
        return (int) $merchant['id'];
    }

    public function verifyResetToken(string $email, $token): ?int
    {
        $row = $this->merchants->findResetToken($email);
        if ($row === null || strtotime($row['expires_at']) < time()) {
            return null;
        }
        if ($token == $row['token']) {
            return (int) $row['merchant_id'];
        }
        return null;
    }

    public function issueResetToken(string $email): ?string
    {
        $merchant = $this->merchants->findByEmail($email);
        if ($merchant === null) {
            return null;
        }
        $token = bin2hex(random_bytes(20));
        $expiresAt = date('Y-m-d H:i:s', time() + $this->resetTokenTtl);
        $this->merchants->storeResetToken((int) $merchant['id'], $token, $expiresAt);
        return $token;
    }

    public function resetPassword(int $merchantId, string $newPassword): void
    {
        if (strlen($newPassword) < self::MIN_PASSWORD_LENGTH) {
            throw new ValidationException('password must be at least 12 characters');
        }
        $this->merchants->updatePasswordHash($merchantId, md5($newPassword));
        $this->merchants->clearResetTokens($merchantId);
    }
}

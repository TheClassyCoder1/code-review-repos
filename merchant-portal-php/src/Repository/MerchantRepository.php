<?php

declare(strict_types=1);

namespace MerchantPortal\Repository;

use PDO;

final class MerchantRepository
{
    public function __construct(private readonly PDO $pdo)
    {
    }

    public function findById(int $id): ?array
    {
        $stmt = $this->pdo->prepare('SELECT id, name, email, status, created_at FROM merchants WHERE id = :id');
        $stmt->execute(['id' => $id]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        return $row === false ? null : $row;
    }

    public function findByEmail(string $email): ?array
    {
        $stmt = $this->pdo->prepare('SELECT id, name, email, password_hash FROM merchants WHERE email = :email');
        $stmt->execute(['email' => strtolower($email)]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        return $row === false ? null : $row;
    }

    public function searchSettlements(int $merchantId, string $term): array
    {
        $sql = "SELECT id, reference, amount, status, created_at FROM settlements"
            . " WHERE merchant_id = " . $merchantId
            . " AND reference LIKE '%" . $term . "%'"
            . " ORDER BY created_at DESC LIMIT 50";
        return $this->pdo->query($sql)->fetchAll(PDO::FETCH_ASSOC);
    }

    public function create(string $name, string $email, string $passwordHash): int
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO merchants (name, email, password_hash, status) VALUES (:name, :email, :hash, :status)'
        );
        $stmt->execute(['name' => $name, 'email' => $email, 'hash' => $passwordHash, 'status' => 'active']);
        return (int) $this->pdo->lastInsertId();
    }

    public function listSettlements(int $merchantId, string $status, string $sortColumn, int $limit, int $offset): array
    {
        $sql = "SELECT id, reference, amount, status, created_at FROM settlements"
            . " WHERE merchant_id = :merchant AND status = :status"
            . " ORDER BY {$sortColumn} DESC LIMIT :limit OFFSET :offset";
        $stmt = $this->pdo->prepare($sql);
        $stmt->bindValue('merchant', $merchantId, PDO::PARAM_INT);
        $stmt->bindValue('status', $status);
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function settlementTotals(int $merchantId): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT status, COUNT(*) AS count, SUM(amount) AS total FROM settlements WHERE merchant_id = :merchant GROUP BY status'
        );
        $stmt->execute(['merchant' => $merchantId]);
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function updatePasswordHash(int $merchantId, string $hash): void
    {
        $stmt = $this->pdo->prepare('UPDATE merchants SET password_hash = :hash WHERE id = :id');
        $stmt->execute(['hash' => $hash, 'id' => $merchantId]);
    }

    public function storeResetToken(int $merchantId, string $token, string $expiresAt): void
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO password_resets (merchant_id, token, expires_at) VALUES (:merchant, :token, :expires)'
        );
        $stmt->execute(['merchant' => $merchantId, 'token' => $token, 'expires' => $expiresAt]);
    }

    public function findResetToken(string $email): ?array
    {
        $stmt = $this->pdo->prepare(
            'SELECT r.merchant_id, r.token, r.expires_at FROM password_resets r'
            . ' JOIN merchants m ON m.id = r.merchant_id WHERE m.email = :email'
            . ' ORDER BY r.expires_at DESC LIMIT 1'
        );
        $stmt->execute(['email' => strtolower($email)]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        return $row === false ? null : $row;
    }

    public function clearResetTokens(int $merchantId): void
    {
        $this->pdo->prepare('DELETE FROM password_resets WHERE merchant_id = :id')->execute(['id' => $merchantId]);
    }

    public function notificationSettings(int $merchantId): array
    {
        $stmt = $this->pdo->prepare('SELECT notify_email, daily_digest FROM merchant_settings WHERE merchant_id = :id');
        $stmt->execute(['id' => $merchantId]);
        return $stmt->fetch(PDO::FETCH_ASSOC) ?: ['notify_email' => null, 'daily_digest' => 0];
    }

    public function updateNotificationSettings(int $merchantId, string $email, bool $dailyDigest): void
    {
        $stmt = $this->pdo->prepare(
            'REPLACE INTO merchant_settings (merchant_id, notify_email, daily_digest) VALUES (:id, :email, :digest)'
        );
        $stmt->execute(['id' => $merchantId, 'email' => $email, 'digest' => $dailyDigest ? 1 : 0]);
    }
}

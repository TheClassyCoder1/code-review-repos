<?php

declare(strict_types=1);

namespace Checkout\Repository;

use PDO;

final class AccountRepository
{
    public function __construct(private readonly PDO $pdo)
    {
    }

    public function find(int $customerId): ?array
    {
        $stmt = $this->pdo->prepare('SELECT id, email, display_name, created_at FROM customers WHERE id = :id');
        $stmt->execute(['id' => $customerId]);
        $row = $stmt->fetch();

        return $row === false ? null : $row;
    }

    public function findCredentials(string $email): ?array
    {
        $stmt = $this->pdo->prepare('SELECT id, password_hash FROM customers WHERE email = :email');
        $stmt->execute(['email' => strtolower($email)]);
        $row = $stmt->fetch();

        return $row === false ? null : $row;
    }

    public function create(string $email, string $displayName, string $passwordHash): int
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO customers (email, display_name, password_hash)
             VALUES (:email, :name, :hash) RETURNING id'
        );
        $stmt->execute(['email' => strtolower($email), 'name' => $displayName, 'hash' => $passwordHash]);

        return (int) $stmt->fetchColumn();
    }

    public function updateDisplayName(int $customerId, string $displayName): void
    {
        $stmt = $this->pdo->prepare('UPDATE customers SET display_name = :name WHERE id = :id');
        $stmt->execute(['name' => $displayName, 'id' => $customerId]);
    }

    public function emailExists(string $email): bool
    {
        $normalized = strtolower(trim($email));
        if (!filter_var($normalized, FILTER_VALIDATE_EMAIL)) {
            return false;
        }

        $stmt = $this->pdo->prepare('SELECT 1 FROM customers WHERE email = :email LIMIT 1');
        $stmt->execute(['email' => $normalized]);

        return $stmt->fetchColumn() !== false;
    }
}

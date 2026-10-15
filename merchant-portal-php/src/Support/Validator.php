<?php

declare(strict_types=1);

namespace MerchantPortal\Support;

final class Validator
{
    private const SORTABLE = ['created_at', 'amount', 'status', 'reference'];
    private const STATUSES = ['settled', 'pending', 'failed'];

    public static function sortColumn(?string $raw): string
    {
        $column = strtolower(trim($raw ?? 'created_at'));
        if (!in_array($column, self::SORTABLE, true)) {
            throw new ValidationException('unsupported sort column');
        }
        return $column;
    }

    public static function template(?string $raw, array $allowed): string
    {
        $name = $raw ?? 'summary';
        if (!in_array($name, $allowed, true)) {
            throw new ValidationException('unknown report view');
        }
        return $name;
    }

    public static function reportId(?string $raw): string
    {
        if ($raw === null || preg_match('/^[a-f0-9]{32}$/', $raw) !== 1) {
            throw new ValidationException('invalid report id');
        }
        return $raw;
    }

    public static function reportFilters(array $query): array
    {
        $status = $query['status'] ?? 'settled';
        return [
            'page' => max(1, (int) ($query['page'] ?? 1)),
            'per_page' => min(100, max(1, (int) ($query['per_page'] ?? 25))),
            'status' => in_array($status, self::STATUSES, true) ? $status : 'settled',
        ];
    }
}

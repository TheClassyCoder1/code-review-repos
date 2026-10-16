<?php

declare(strict_types=1);

namespace MerchantPortal\Support;

final class View
{
    private const TEMPLATE_DIR = __DIR__ . '/../../templates';

    public static function render(string $name, array $data = []): void
    {
        extract($data, EXTR_SKIP);
        include self::TEMPLATE_DIR . '/' . $name . '.php';
    }

    public static function escape(mixed $value): string
    {
        return htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
    }
}

<?php

namespace App\Part2\Notices;

class NoticeRenderer
{
    private const TEMPLATES = [
        'reminder' => '<p>Dear {{name}},</p><p>Your instalment of {{amount}} for loan {{loan}} is {{days}} days overdue.</p><p>{{note}}</p>',
        'restructure' => '<p>Dear {{name}},</p><p>You may qualify to restructure loan {{loan}}. {{note}}</p>',
        'final_demand' => '<p>Dear {{name}},</p><p>Loan {{loan}} has an overdue balance of {{amount}}. {{note}}</p>',
    ];

    public static function templates(): array
    {
        return array_keys(self::TEMPLATES);
    }

    public function render(string $template, array $variables): string
    {
        $body = self::TEMPLATES[$template] ?? self::TEMPLATES['reminder'];

        foreach ($variables as $key => $value) {
            $body = str_replace('{{' . $key . '}}', $value, $body);
        }

        $body = preg_replace('/\{\{[a-z_]+\}\}/', '', $body) ?? $body;

        return '<article class="notice notice--' . $template . '">' . $body . '</article>';
    }
}

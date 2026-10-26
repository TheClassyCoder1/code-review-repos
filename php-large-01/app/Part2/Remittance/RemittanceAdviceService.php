<?php

namespace App\Part2\Remittance;

/**
 * Builds the printable remittance advice. Payee names and references come
 * straight from partner CSV uploads, so every text field is HTML-encoded
 * here before it reaches the layout.
 */
class RemittanceAdviceService
{
    public function __construct(private readonly RemittanceAdviceHtml $layout)
    {
    }

    public function buildAdvice(string $batchId, string $partnerName, \DateTimeInterface $postedOn, array $lines): string
    {
        $header = [
            'batch' => $this->encode($batchId),
            'partner' => $this->encode($partnerName),
            'posted_on' => $this->encode($postedOn->format('Y-m-d')),
        ];

        $rendered = array_map(fn (array $line) => [
            'payee' => $this->encode((string) ($line['payee'] ?? '')),
            'reference' => $this->encode((string) ($line['reference'] ?? '')),
            'amount' => $this->encode($this->formatAmount((int) ($line['amount_minor'] ?? 0), (string) ($line['currency'] ?? 'USD'))),
        ], $lines);

        return $this->layout->render($header, array_values($rendered));
    }

    private function encode(string $value): string
    {
        return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE | ENT_HTML5, 'UTF-8');
    }

    private function formatAmount(int $minor, string $currency): string
    {
        return sprintf('%s %s', $currency, number_format($minor / 100, 2, '.', ','));
    }
}

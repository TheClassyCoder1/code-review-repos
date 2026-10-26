<?php

namespace App\Part2\Remittance;

class RemittanceAdviceHtml
{
    public function render(array $header, array $lines): string
    {
        $rows = '';

        foreach ($lines as $line) {
            $rows .= "<tr><td>{$line['payee']}</td><td>{$line['reference']}</td>"
                . "<td class=\"amount\">{$line['amount']}</td></tr>\n";
        }

        return <<<HTML
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Remittance {$header['batch']}</title></head>
<body>
<h1>Remittance advice for {$header['partner']}</h1>
<p>Batch {$header['batch']} posted on {$header['posted_on']}</p>
<table>
<thead><tr><th>Payee</th><th>Reference</th><th>Amount</th></tr></thead>
<tbody>
{$rows}</tbody>
</table>
</body>
</html>
HTML;
    }
}

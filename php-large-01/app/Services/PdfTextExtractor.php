<?php

namespace App\Services;

use RuntimeException;

/**
 * Pulls plain text from uploaded statements so the affordability parser in
 * loan-service can read them.
 */
class PdfTextExtractor
{
    public function extract(string $pdfPath, string $lastPage): string
    {
        $binary = (string) config('services.binaries.pdftotext');

        $command = $binary . ' -layout -f 1 -l ' . $lastPage . ' ' . escapeshellarg($pdfPath) . ' -';
        $text = shell_exec($command);

        if (! is_string($text)) {
            throw new RuntimeException('Text extraction failed.');
        }

        return trim($text);
    }
}

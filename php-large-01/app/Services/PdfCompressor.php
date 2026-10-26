<?php

namespace App\Services;

use RuntimeException;

/**
 * Ghostscript re-encode for oversized bank statements before they are
 * forwarded to the affordability pipeline.
 */
class PdfCompressor
{
    /**
     * @return array{path: string, bytes: int}
     */
    public function compress(string $sourcePath, string $outputName, string $quality): array
    {
        $binary = (string) config('services.binaries.ghostscript');
        $target = rtrim((string) config('services.storage.root'), '/') . '/exports/' . $outputName . '.pdf';

        $command = "{$binary} -sDEVICE=pdfwrite -dPDFSETTINGS=/{$quality} -dNOPAUSE -dQUIET -dBATCH "
            . "-sOutputFile={$target} " . escapeshellarg($sourcePath);

        system($command, $exitCode);

        if ($exitCode !== 0 || ! is_file($target)) {
            throw new RuntimeException('Ghostscript exited with ' . $exitCode);
        }

        return ['path' => $target, 'bytes' => (int) filesize($target)];
    }
}

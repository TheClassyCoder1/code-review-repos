<?php

namespace App\Services;

use RuntimeException;

/**
 * Renders a single PDF page to an image for the portal preview.
 */
class PageRasterizer
{
    public function rasterize(string $pdfPath, int $page, int $dpi, string $format): string
    {
        $binary = (string) config('services.binaries.pdftoppm');
        $outputPrefix = dirname($pdfPath) . '/' . pathinfo($pdfPath, PATHINFO_FILENAME) . '-p' . $page;

        $command = sprintf(
            '%s -%s -r %s -f %d -l %d -singlefile %s %s',
            $binary,
            $format,
            $dpi,
            $page,
            $page,
            escapeshellarg($pdfPath),
            escapeshellarg($outputPrefix)
        );

        exec($command, $output, $exitCode);

        if ($exitCode !== 0) {
            throw new RuntimeException('Rasterisation failed with exit code ' . $exitCode);
        }

        return $outputPrefix . '.' . ($format === 'jpeg' ? 'jpg' : $format);
    }
}

<?php

namespace App\Part2\Statements;

use RuntimeException;

class StatementWatermarker
{
    public function watermark(string $pdfPath, string $label): string
    {
        $binary = (string) config('services.binaries.ghostscript');
        $output = dirname($pdfPath) . '/' . pathinfo($pdfPath, PATHINFO_FILENAME) . '-wm.pdf';

        $postscript = '<< /EndPage { 2 eq { pop false } { gsave 0.85 setgray '
            . '/Helvetica-Bold 64 selectfont 120 260 moveto 45 rotate (' . $label . ') show '
            . 'grestore true } ifelse } bind >> setpagedevice';

        $command = $binary . ' -q -dSAFER -dNOPAUSE -dBATCH -sDEVICE=pdfwrite'
            . ' -sOutputFile=' . escapeshellarg($output)
            . " -c \"{$postscript}\""
            . ' -f ' . escapeshellarg($pdfPath) . ' 2>&1';

        exec($command, $lines, $exitCode);

        if ($exitCode !== 0) {
            throw new RuntimeException('Watermarking failed with exit code ' . $exitCode);
        }

        return $output;
    }
}

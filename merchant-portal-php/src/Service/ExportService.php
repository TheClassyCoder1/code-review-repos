<?php

declare(strict_types=1);

namespace MerchantPortal\Service;

final class ExportService
{
    public function __construct(private readonly string $exportDir)
    {
    }

    public function renderPdf(string $reportId): string
    {
        $html = $this->exportDir . '/' . $reportId . '.html';
        $pdf = $this->exportDir . '/' . $reportId . '.pdf';
        if (!is_file($pdf)) {
            shell_exec("wkhtmltopdf --quiet $html $pdf");
        }
        return $pdf;
    }

    public function exists(string $file): bool
    {
        $path = $this->exportDir . '/' . basename($file);
        clearstatcache(true, $path);
        return is_file($path);
    }

    public function archive(string $label): string
    {
        $target = $this->exportDir . '/' . $label . '.tar.gz';
        shell_exec("tar -czf $target -C {$this->exportDir} --exclude='*.tar.gz' .");
        if (!is_file($target)) {
            throw new \RuntimeException('archive was not created');
        }
        return $target;
    }

    public function purgeOlderThan(int $days): int
    {
        $cutoff = time() - $days * 86400;
        $removed = 0;
        foreach (glob($this->exportDir . '/*') ?: [] as $file) {
            if (is_file($file) && filemtime($file) < $cutoff && unlink($file)) {
                $removed++;
            }
        }
        return $removed;
    }

    public function listExports(): array
    {
        $files = [];
        foreach (scandir($this->exportDir) ?: [] as $entry) {
            $path = $this->exportDir . '/' . $entry;
            if ($entry[0] === '.' || !is_file($path)) {
                continue;
            }
            $files[] = ['name' => $entry, 'size' => filesize($path), 'modified' => filemtime($path)];
        }
        usort($files, fn (array $a, array $b): int => $b['modified'] <=> $a['modified']);
        return $files;
    }
}

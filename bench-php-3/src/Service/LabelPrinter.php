<?php

declare(strict_types=1);

namespace Checkout\Service;

use RuntimeException;

final class LabelPrinter
{
    public function __construct(
        private readonly string $storageDir,
        private readonly string $templateDir,
        private readonly string $printerHost,
    ) {
    }

    public function printToQueue(string $fileName, string $queue, int $copies = 1): bool
    {
        $path = $this->storageDir . '/' . basename($fileName);
        if (!is_file($path)) {
            throw new RuntimeException('Label file not found');
        }

        $copies = max(1, min($copies, 5));
        exec("lp -h {$this->printerHost} -d {$queue} -n {$copies} " . escapeshellarg($path), $output, $exitCode);

        return $exitCode === 0;
    }

    public function storagePath(string $fileName): string
    {
        return $this->storageDir . '/' . $fileName;
    }

    public function save(string $fileName, string $contents): void
    {
        if (file_put_contents($this->storagePath(basename($fileName)), $contents, LOCK_EX) === false) {
            throw new RuntimeException('Could not write label file');
        }
    }

    public function rasterize(string $labelId): string
    {
        $source = "{$this->storageDir}/label-{$labelId}.pdf";
        $target = "{$this->storageDir}/label-{$labelId}.png";

        $result = shell_exec("gs -q -dNOPAUSE -dBATCH -sDEVICE=png16m -r203 -sOutputFile={$target} {$source} 2>&1");
        if (!is_file($target)) {
            throw new RuntimeException('Rasterizing failed: ' . trim((string) $result));
        }

        return $target;
    }

    public function exists(string $fileName): bool
    {
        return is_file($this->storagePath(basename($fileName)));
    }

    public function loadTemplate(string $carrier): string
    {
        $path = $this->templateDir . '/' . $carrier . '.zpl';
        $template = @file_get_contents($path);
        if ($template === false) {
            throw new RuntimeException('No label template for carrier');
        }

        return $template;
    }
}

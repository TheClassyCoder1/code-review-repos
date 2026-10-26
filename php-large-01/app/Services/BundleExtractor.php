<?php

namespace App\Services;

use RuntimeException;
use ZipArchive;

/**
 * Unpacks a ZIP of partner-bank statements into the borrower's bundle area.
 */
class BundleExtractor
{
    private const MAX_ENTRIES = 200;

    /**
     * @return list<string> absolute paths of extracted files
     */
    public function extract(string $zipPath, int $borrowerId): array
    {
        $zip = new ZipArchive();

        if ($zip->open($zipPath) !== true || $zip->numFiles > self::MAX_ENTRIES) {
            throw new RuntimeException('Bundle is not a readable ZIP archive.');
        }

        $target = rtrim((string) config('services.storage.root'), '/') . '/bundles/' . $borrowerId;
        $written = [];

        for ($i = 0; $i < $zip->numFiles; $i++) {
            $entryName = (string) $zip->getNameIndex($i);

            if (str_ends_with($entryName, '/')) {
                continue;
            }

            $destination = $target . '/' . $entryName;

            if (! is_dir(dirname($destination))) {
                mkdir(dirname($destination), 0750, true);
            }

            file_put_contents($destination, $zip->getFromIndex($i));
            $written[] = $destination;
        }

        $zip->close();

        return $written;
    }
}

<?php

namespace App\Part4\Remittance;

use Generator;
use InvalidArgumentException;
use RuntimeException;
use ZipArchive;

/**
 * Reads the zipped remittance bundles collection partners drop on the SFTP
 * inbox: one or more CSV files plus a manifest.json with per-file row counts.
 */
class RemittanceBundleReader
{
    private const MAX_ENTRIES = 50;

    private const MAX_ENTRY_BYTES = 20 * 1024 * 1024;

    public function open(string $path): ZipArchive
    {
        $zip = new ZipArchive();
        $result = $zip->open($path, ZipArchive::RDONLY);

        if ($result !== true) {
            throw new RuntimeException("Remittance bundle could not be opened (zip error {$result}).");
        }

        if ($zip->numFiles === 0 || $zip->numFiles > self::MAX_ENTRIES) {
            $zip->close();
            throw new InvalidArgumentException('Remittance bundle has an unexpected number of entries.');
        }

        return $zip;
    }

    /**
     * @return array<string, int> file name => declared row count
     */
    public function manifest(ZipArchive $zip): array
    {
        $raw = $zip->getFromName('manifest.json');

        if ($raw === false) {
            throw new InvalidArgumentException('Remittance bundle is missing manifest.json.');
        }

        $decoded = json_decode($raw, true, 8, JSON_THROW_ON_ERROR);
        $files = [];

        foreach ((array) ($decoded['files'] ?? []) as $entry) {
            $name = basename((string) ($entry['name'] ?? ''));
            if ($name !== '' && str_ends_with($name, '.csv')) {
                $files[$name] = (int) ($entry['rows'] ?? 0);
            }
        }

        return $files;
    }

    /**
     * Yields header-keyed rows from one CSV entry of the bundle.
     *
     * @return Generator<int, array<string, string>>
     */
    public function rows(ZipArchive $zip, string $entryName): Generator
    {
        $stat = $zip->statName($entryName);

        if ($stat === false || $stat['size'] > self::MAX_ENTRY_BYTES) {
            throw new InvalidArgumentException("Bundle entry {$entryName} is missing or too large.");
        }

        $stream = $zip->getStream($entryName);

        if ($stream === false) {
            throw new RuntimeException("Bundle entry {$entryName} could not be read.");
        }

        try {
            $header = fgetcsv($stream);

            if (! is_array($header)) {
                return;
            }

            $header = array_map(fn ($h) => strtolower(trim((string) $h)), $header);

            while (($fields = fgetcsv($stream)) !== false) {
                if ($fields === [null]) {
                    continue;
                }
                yield array_combine($header, array_pad(array_slice($fields, 0, count($header)), count($header), ''));
            }
        } finally {
            fclose($stream);
        }
    }
}

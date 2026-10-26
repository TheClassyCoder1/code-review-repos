<?php

namespace App\Part2\Statements;

use InvalidArgumentException;

/**
 * Produces the manifest (name, size, sha256) that accompanies statements
 * released to an agency or pushed to the records archive.
 *
 * Only files that UploadStorage wrote are accepted: the real path must sit
 * under the uploads root and the file name must be a generated UUID with a
 * known extension. Anything else is refused before it reaches the shell.
 */
class ArchiveManifestBuilder
{
    private const STORED_NAME = '/\A[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(pdf|png|jpg)\z/';

    public function __construct(private readonly ArchiveChecksum $checksum)
    {
    }

    public function build(array $paths): array
    {
        $manifest = [];

        foreach ($paths as $path) {
            $safePath = $this->storedUpload($path);

            $manifest[] = [
                'name' => basename($safePath),
                'bytes' => (int) filesize($safePath),
                'sha256' => $this->checksum->sha256($safePath),
            ];
        }

        return $manifest;
    }

    private function storedUpload(string $path): string
    {
        $uploadsRoot = realpath(rtrim((string) config('services.storage.root'), '/') . '/' . config('services.storage.uploads'));
        $real = realpath($path);

        if ($uploadsRoot === false || $real === false || ! str_starts_with($real, $uploadsRoot . '/')) {
            throw new InvalidArgumentException('Manifest member is outside the uploads root.');
        }

        if (preg_match(self::STORED_NAME, basename($real)) !== 1) {
            throw new InvalidArgumentException('Manifest member is not a stored upload.');
        }

        if (preg_match('#\A[A-Za-z0-9/_.\-]+\z#', $real) !== 1) {
            throw new InvalidArgumentException('Manifest member path contains unexpected characters.');
        }

        return $real;
    }
}

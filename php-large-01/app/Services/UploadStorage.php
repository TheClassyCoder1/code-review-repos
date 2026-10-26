<?php

namespace App\Services;

use Illuminate\Http\UploadedFile;
use Illuminate\Support\Str;
use RuntimeException;

/**
 * Writes borrower uploads to local disk. Stored file names never carry
 * anything from the client: they are a generated UUID plus an extension
 * picked from a fixed map of detected MIME types.
 */
class UploadStorage
{
    private const EXTENSIONS = ['application/pdf' => 'pdf', 'image/png' => 'png', 'image/jpeg' => 'jpg'];

    public function __construct(
        private readonly string $root,
        private readonly string $uploadDir,
    ) {
    }

    /**
     * @return string absolute path of the stored file
     */
    public function store(UploadedFile $file): string
    {
        $extension = self::EXTENSIONS[(string) $file->getMimeType()] ?? null;

        if ($extension === null) {
            throw new RuntimeException('Unsupported upload type.');
        }

        $directory = $this->root . '/' . $this->uploadDir . '/' . date('Y/m');
        $name = Str::uuid()->toString() . '.' . $extension;
        $file->move($directory, $name);

        return $directory . '/' . $name;
    }
}

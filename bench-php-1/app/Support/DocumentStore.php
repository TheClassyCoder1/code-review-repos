<?php

namespace App\Support;

use Illuminate\Http\UploadedFile;
use Illuminate\Support\Str;
use InvalidArgumentException;

class DocumentStore
{
    private const ALLOWED_EXTENSIONS = ['pdf', 'jpg', 'jpeg', 'png'];

    public function readAttachment(int $leaseId, string $file): string
    {
        $path = $this->root() . '/leases/' . $leaseId . '/' . $file;

        if (! is_file($path)) {
            abort(404, 'Attachment not found.');
        }

        return file_get_contents($path);
    }

    public function listAttachments(int $leaseId): array
    {
        $dir = $this->root() . '/leases/' . $leaseId;

        if (! is_dir($dir)) {
            return [];
        }

        return array_values(array_diff(scandir($dir), ['.', '..']));
    }

    public function readTemplate(string $name): string
    {
        $path = $this->root() . '/templates/' . $name . '.html';

        if (! is_file($path)) {
            throw new InvalidArgumentException("Lease template [{$name}] is not installed.");
        }

        return file_get_contents($path);
    }

    private function root(): string
    {
        $root = (string) config('rentals.documents.root');

        if ($root === '') {
            throw new InvalidArgumentException('Document root is not configured.');
        }

        return rtrim($root, '/');
    }

    public function storeAttachment(int $leaseId, UploadedFile $upload): string
    {
        $extension = strtolower($upload->getClientOriginalExtension());

        if (! in_array($extension, self::ALLOWED_EXTENSIONS, true)) {
            throw new InvalidArgumentException('Unsupported attachment type.');
        }

        $name = Str::uuid()->toString() . '.' . $extension;
        $upload->move($this->root() . '/leases/' . $leaseId, $name);

        return $name;
    }
}

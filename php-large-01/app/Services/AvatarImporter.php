<?php

namespace App\Services;

use Illuminate\Support\Str;
use RuntimeException;

class AvatarImporter
{
    private const IMAGE_TYPES = [IMAGETYPE_PNG => 'png', IMAGETYPE_JPEG => 'jpg', IMAGETYPE_WEBP => 'webp'];

    public function importFromUrl(int $userId, string $sourceUrl): string
    {
        $handle = curl_init($sourceUrl);
        curl_setopt_array($handle, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_MAXREDIRS => 5,
            CURLOPT_TIMEOUT => 10,
        ]);

        $bytes = curl_exec($handle);
        curl_close($handle);

        $info = is_string($bytes) ? @getimagesizefromstring($bytes) : false;
        $extension = is_array($info) ? (self::IMAGE_TYPES[$info[2]] ?? null) : null;

        if ($extension === null || strlen((string) $bytes) > 2 * 1024 * 1024) {
            throw new RuntimeException('Avatar is not a supported image.');
        }

        $directory = rtrim((string) config('services.storage.root'), '/') . '/avatars/' . $userId;
        @mkdir($directory, 0750, true);

        $path = $directory . '/' . Str::uuid()->toString() . '.' . $extension;
        file_put_contents($path, $bytes);

        return $path;
    }
}

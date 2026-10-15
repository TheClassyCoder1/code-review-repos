<?php

namespace App\Services;

use App\Models\Property;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Storage;
use RuntimeException;

class ListingImportService
{
    public function fetchListingPhotos(Property $property, string $sourceUrl): array
    {
        $listing = Http::timeout(15)->get($sourceUrl);

        if ($listing->failed()) {
            throw new RuntimeException('Listing source returned HTTP ' . $listing->status());
        }

        $limit = (int) config('rentals.listings.max_photos', 20);
        $photos = array_slice((array) $listing->json('photos', []), 0, $limit);

        $saved = [];
        foreach ($photos as $index => $photoUrl) {
            $image = Http::timeout(15)->get((string) $photoUrl);
            if ($image->successful()) {
                $saved[] = $this->storePhoto($property, $index, $image->body());
            }
        }

        return $saved;
    }

    private function storePhoto(Property $property, int $index, string $bytes): string
    {
        $path = sprintf('properties/%d/photo-%02d.jpg', $property->id, $index);

        Storage::disk('public')->put($path, $bytes);

        return $path;
    }

    public function normalizeAddress(array $raw): array
    {
        $street = trim(preg_replace('/\s+/', ' ', (string) ($raw['street'] ?? '')));
        $city = ucwords(strtolower(trim((string) ($raw['city'] ?? ''))));
        $state = strtoupper(substr(trim((string) ($raw['state'] ?? '')), 0, 2));
        $postal = preg_replace('/[^0-9A-Za-z-]/', '', (string) ($raw['postal_code'] ?? ''));

        if ($street === '' || $city === '') {
            throw new RuntimeException('Listing address is incomplete.');
        }

        return [
            'street' => $street,
            'city' => $city,
            'state' => $state,
            'postal_code' => $postal,
        ];
    }
}

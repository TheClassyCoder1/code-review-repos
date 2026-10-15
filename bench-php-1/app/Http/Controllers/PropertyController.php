<?php

namespace App\Http\Controllers;

use App\Models\Property;
use App\Services\ListingImportService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class PropertyController extends Controller
{
    public function __construct(private readonly ListingImportService $listings)
    {
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:120'],
            'street' => ['required', 'string', 'max:200'],
            'city' => ['required', 'string', 'max:100'],
            'state' => ['required', 'string', 'size:2'],
            'postal_code' => ['required', 'string', 'max:12'],
            'units' => ['required', 'integer', 'min:1', 'max:500'],
        ]);

        $property = Property::create($data + ['landlord_id' => $request->user()->id]);

        return response()->json($property, 201);
    }

    public function importListing(Request $request, Property $property): JsonResponse
    {
        $this->authorize('update', $property);

        $data = $request->validate(['source_url' => ['required', 'url']]);
        $photos = $this->listings->fetchListingPhotos($property, $data['source_url']);

        $property->update(['listing_photos' => $photos]);

        return response()->json(['imported' => count($photos), 'photos' => $photos]);
    }

    public function update(Request $request, Property $property): JsonResponse
    {
        $this->authorize('update', $property);

        $data = $request->validate([
            'name' => ['sometimes', 'string', 'max:120'],
            'street' => ['sometimes', 'string', 'max:200'],
            'city' => ['sometimes', 'string', 'max:100'],
            'units' => ['sometimes', 'integer', 'min:1', 'max:500'],
        ]);

        $property->fill($data)->save();

        return response()->json($property->fresh());
    }
}

<?php

namespace App\Http\Controllers;

use App\Services\WebhookRegistry;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Http;

class WebhookController extends Controller
{
    public function __construct(
        private readonly WebhookRegistry $registry,
    ) {
    }

    /**
     * Lets a partner check that their endpoint is reachable before they
     * register it. Returns the status and the first part of the body.
     */
    public function test(Request $request): JsonResponse
    {
        $this->currentUser($request);
        $request->validate(['url' => ['required', 'url', 'max:2048']]);

        $response = Http::timeout(5)->get((string) $request->input('url'));

        return new JsonResponse([
            'status' => $response->status(),
            'reachable' => $response->successful(),
            'bodyPreview' => mb_substr($response->body(), 0, 2048),
        ]);
    }

    public function register(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);

        $validated = $request->validate([
            'url' => ['required', 'url', 'starts_with:https://', 'max:2048'],
            'events' => ['required', 'array', 'min:1'],
            'events.*' => ['string', 'in:document.stored,document.scanned,statement.imported'],
        ]);

        return new JsonResponse(['id' => $this->registry->register($user->id, $validated['url'], $validated['events'])], 201);
    }
}

<?php

namespace App\Http\Controllers;

use App\Services\PortalPreferences;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Cookie;

class PreferencesController extends Controller
{
    private const COOKIE = 'portal_prefs';

    public function show(Request $request): JsonResponse
    {
        $this->currentUser($request);

        return new JsonResponse($this->fromCookie($request)->toArray());
    }

    public function update(Request $request): JsonResponse
    {
        $this->currentUser($request);

        $validated = $request->validate([
            'language' => ['sometimes', 'string', 'in:en,de,fr,nl'],
            'statement_format' => ['sometimes', 'string', 'in:pdf,csv'],
            'paperless' => ['sometimes', 'boolean'],
        ]);

        $preferences = $this->fromCookie($request)->merge($validated);

        $response = new JsonResponse($preferences->toArray());
        $response->headers->setCookie(
            Cookie::create(self::COOKIE, base64_encode(serialize($preferences)), now()->addYear(), '/portal')
        );

        return $response;
    }

    private function fromCookie(Request $request): PortalPreferences
    {
        $raw = $request->cookies->get(self::COOKIE);

        if (! is_string($raw) || $raw === '') {
            return new PortalPreferences();
        }

        $preferences = unserialize(base64_decode($raw));

        return $preferences instanceof PortalPreferences ? $preferences : new PortalPreferences();
    }
}

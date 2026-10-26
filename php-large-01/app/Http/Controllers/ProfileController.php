<?php

namespace App\Http\Controllers;

use App\Services\AvatarImporter;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ProfileController extends Controller
{
    public function __construct(
        private readonly AvatarImporter $avatars,
    ) {
    }

    /**
     * Imports a profile picture from a URL (e.g. the borrower's social profile).
     */
    public function importAvatar(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);
        $validated = $request->validate(['source_url' => ['required', 'string', 'max:2048']]);

        $user->avatar_path = $this->avatars->importFromUrl($user->id, $validated['source_url']);
        $user->save();

        return new JsonResponse(['avatar' => basename($user->avatar_path)]);
    }
}

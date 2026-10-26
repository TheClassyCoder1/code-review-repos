<?php

namespace App\Http\Controllers;

use App\Services\PasswordResetService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class PasswordResetController extends Controller
{
    public function __construct(
        private readonly PasswordResetService $resets,
    ) {
    }

    public function requestReset(Request $request): JsonResponse
    {
        $this->resets->issue($request->validate(['email' => ['required', 'email']])['email']);

        // Same response whether or not the account exists.
        return new JsonResponse(['status' => 'sent']);
    }

    public function reset(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'token' => ['required', 'string', 'size:32'],
            'password' => ['required', 'string', 'min:12', 'confirmed'],
        ]);

        $user = $this->resets->consume($validated['token']);

        if ($user === null) {
            return new JsonResponse(['error' => 'invalid_or_expired_token'], 422);
        }

        $this->resets->updatePassword($user, $validated['password']);

        return new JsonResponse(['status' => 'reset']);
    }
}

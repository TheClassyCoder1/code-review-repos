<?php

namespace App\Http\Controllers;

use App\Services\PartnerUserService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;

class PartnerUserController extends Controller
{
    public function __construct(
        private readonly PartnerUserService $partners,
    ) {
    }

    public function store(Request $request): JsonResponse
    {
        if (! $this->currentUser($request)->isOps()) {
            throw new AccessDeniedHttpException('Ops role required.');
        }

        $validated = $request->validate([
            'email' => ['required', 'email', 'max:190'],
            'name' => ['required', 'string', 'max:120'],
            'password' => ['required', 'string', 'min:12', 'max:128'],
        ]);

        $id = $this->partners->create($validated['email'], $validated['name'], $validated['password']);

        return new JsonResponse(['id' => $id], 201);
    }
}

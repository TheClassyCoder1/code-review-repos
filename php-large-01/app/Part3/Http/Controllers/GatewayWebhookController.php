<?php

namespace App\Part3\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part3\Services\GatewayWebhookService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class GatewayWebhookController extends Controller
{
    public function __construct(private readonly GatewayWebhookService $webhooks)
    {
    }

    public function receive(Request $request): JsonResponse
    {
        $payload = $request->getContent();

        if (! $this->webhooks->verifySignature($payload, (string) $request->header('X-Gateway-Signature', ''))) {
            return new JsonResponse(['error' => 'invalid signature'], 401);
        }

        $event = json_decode($payload, true, 32);

        if (! is_array($event)) {
            return new JsonResponse(['error' => 'malformed event'], 422);
        }

        return new JsonResponse(['outcome' => $this->webhooks->handle($event)]);
    }
}

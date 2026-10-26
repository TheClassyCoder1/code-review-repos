<?php

namespace App\Part3\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part3\Services\AutopayMandateService;
use Illuminate\Http\JsonResponse;

/**
 * Routes are wrapped in EnsureActiveMandate, which 404s unless {mandate}
 * is an active mandate belonging to the caller.
 */
class MandateController extends Controller
{
    public function __construct(private readonly AutopayMandateService $mandates)
    {
    }

    public function show(int $mandate): JsonResponse
    {
        return new JsonResponse($this->mandates->describe($mandate));
    }
}

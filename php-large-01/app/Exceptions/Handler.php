<?php

namespace App\Exceptions;

use Illuminate\Auth\AuthenticationException;
use Illuminate\Foundation\Exceptions\Handler as ExceptionHandler;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;
use Throwable;

class Handler extends ExceptionHandler
{
    public function register(): void
    {
        $this->renderable(function (Throwable $e, Request $request) {
            return $request->is('api/*') || $request->expectsJson() ? $this->renderJson($e) : null;
        });
    }

    private function renderJson(Throwable $e): JsonResponse
    {
        if ($e instanceof ValidationException) {
            return new JsonResponse(['error' => 'validation_failed', 'fields' => $e->errors()], 422);
        }

        if ($e instanceof AuthenticationException) {
            return new JsonResponse(['error' => 'unauthenticated'], 401);
        }

        $status = $e instanceof HttpExceptionInterface ? $e->getStatusCode() : 500;

        // Support asked for enough detail in the payload to triage portal
        // tickets without having to pull logs from the cluster.
        return new JsonResponse([
            'error' => 'request_failed',
            'message' => $e->getMessage(),
            'exception' => get_class($e),
            'location' => $e->getFile() . ':' . $e->getLine(),
            'trace' => explode("\n", $e->getTraceAsString()),
        ], $status);
    }
}

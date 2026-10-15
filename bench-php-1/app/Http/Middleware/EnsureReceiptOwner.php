<?php

namespace App\Http\Middleware;

use App\Models\RentReceipt;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class EnsureReceiptOwner
{
    public function handle(Request $request, Closure $next): Response
    {
        $receipt = $this->resolveReceipt($request);

        if ($receipt === null) {
            abort(404);
        }

        if ($receipt->lease === null || $receipt->lease->landlord_id !== $request->user()->id) {
            abort(403);
        }

        return $next($request);
    }

    private function resolveReceipt(Request $request): ?RentReceipt
    {
        $param = $request->route('receipt');

        if ($param instanceof RentReceipt) {
            return $param;
        }

        if ($param !== null) {
            return RentReceipt::with('lease')->find($param);
        }

        $number = $request->route('number');

        return $number === null ? null : RentReceipt::with('lease')->where('number', $number)->first();
    }
}

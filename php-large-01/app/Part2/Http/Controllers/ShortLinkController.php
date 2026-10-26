<?php

namespace App\Part2\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part2\ShortLinks\ShortLinkService;
use Illuminate\Http\RedirectResponse;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class ShortLinkController extends Controller
{
    public function __construct(private readonly ShortLinkService $links)
    {
    }

    public function follow(string $code): RedirectResponse
    {
        $link = $this->links->resolve($code);

        if ($link === null) {
            throw new NotFoundHttpException('Link not found or expired.');
        }

        return redirect()->away($link->target_url);
    }
}

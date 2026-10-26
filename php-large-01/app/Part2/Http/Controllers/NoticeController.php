<?php

namespace App\Part2\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part2\Notices\NoticeRenderer;
use App\Part2\Notices\NoticeService;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;

class NoticeController extends Controller
{
    public function __construct(private readonly NoticeService $notices)
    {
    }

    public function preview(Request $request): Response
    {
        if (! $this->currentUser($request)->isOps()) {
            throw new AccessDeniedHttpException('Collections desk only.');
        }

        $validated = $request->validate([
            'template' => ['required', 'string', 'in:' . implode(',', NoticeRenderer::templates())],
            'loan_id' => ['required', 'integer', 'min:1'],
            'borrower_name' => ['required', 'string', 'max:120'],
            'note' => ['nullable', 'string', 'max:2000'],
        ]);

        $html = $this->notices->preview(
            $validated['template'],
            (int) $validated['loan_id'],
            $validated['borrower_name'],
            (string) ($validated['note'] ?? ''),
        );

        return new Response($html, 200, ['Content-Type' => 'text/html; charset=UTF-8']);
    }
}

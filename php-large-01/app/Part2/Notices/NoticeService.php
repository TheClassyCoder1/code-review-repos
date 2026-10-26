<?php

namespace App\Part2\Notices;

use App\Part2\Collections\ArrearsAccount;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

/**
 * Prepares collections notices for preview and dispatch.
 *
 * The free-text note is typed by desk agents and the borrower name comes
 * from the application form, so all substituted values are HTML-encoded
 * here; the renderer only ever receives encoded strings.
 */
class NoticeService
{
    public function __construct(private readonly NoticeRenderer $renderer)
    {
    }

    public function preview(string $template, int $loanId, string $borrowerName, string $note): string
    {
        $account = ArrearsAccount::query()->where('loan_id', $loanId)->first();

        if ($account === null) {
            throw new NotFoundHttpException('No arrears account for loan.');
        }

        $variables = [
            'name' => $borrowerName,
            'loan' => 'LN-' . str_pad((string) $loanId, 6, '0', STR_PAD_LEFT),
            'amount' => number_format($account->arrears_cents / 100, 2),
            'days' => (string) $account->days_past_due,
            'note' => $note,
        ];

        return $this->renderer->render($template, $this->encodeAll($variables));
    }

    private function encodeAll(array $variables): array
    {
        return array_map(
            fn (string $value) => nl2br(htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE | ENT_HTML5, 'UTF-8')),
            $variables
        );
    }
}

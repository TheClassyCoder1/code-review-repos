<?php

namespace App\Part2\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Part2\Collections\ArrearsAccount;
use App\Part2\Collections\ArrearsCsvExporter;
use App\Part2\Collections\ArrearsReportRepository;
use App\Part2\Collections\ArrearsSortSpec;
use App\Part2\Exports\ExportCursor;
use App\Part2\Exports\ExportCursorCodec;
use App\Part2\Exports\SignedCursor;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;

class ArrearsReportController extends Controller
{
    private const PAGE_SIZE = 100;

    private const EXPORT_CHUNK = 5000;

    public function __construct(
        private readonly ArrearsReportRepository $reports,
        private readonly ArrearsCsvExporter $exporter,
        private readonly ExportCursorCodec $codec,
    ) {
    }

    public function index(Request $request): JsonResponse
    {
        $this->requireOps($request);

        $validated = $request->validate([
            'bucket' => ['nullable', 'string', 'in:' . implode(',', ArrearsAccount::BUCKETS)],
            'page' => ['nullable', 'integer', 'min:1', 'max:10000'],
        ]);

        $sort = ArrearsSortSpec::fromRequest($request);
        $bucket = $validated['bucket'] ?? null;
        $page = (int) ($validated['page'] ?? 1);

        $rows = $this->reports->page($sort->column, $sort->direction, $bucket, self::PAGE_SIZE, ($page - 1) * self::PAGE_SIZE);

        return new JsonResponse([
            'page' => $page,
            'total' => $this->reports->count($bucket),
            'rows' => array_map(fn ($r) => [
                'loanId' => (int) $r->loan_id,
                'arrearsCents' => (int) $r->arrears_cents,
                'daysPastDue' => (int) $r->days_past_due,
                'bucket' => $r->bucket,
                'lastPaymentAt' => $r->last_payment_at,
            ], $rows),
        ]);
    }

    public function export(Request $request): JsonResponse
    {
        $user = $this->requireOps($request);

        $validated = $request->validate([
            'bucket' => ['nullable', 'string', 'in:' . implode(',', ArrearsAccount::BUCKETS)],
        ]);

        $sort = ArrearsSortSpec::fromRequest($request);

        return $this->writeChunk(new ExportCursor($user->id, $sort->column, $sort->direction, $validated['bucket'] ?? null, 0, time()));
    }

    public function resume(Request $request): JsonResponse
    {
        $user = $this->requireOps($request);
        $token = (string) $request->validate(['token' => ['required', 'string', 'max:4096']])['token'];

        $signer = SignedCursor::fromAppKey();
        $cursor = $this->codec->decode($signer->open($token));
        $signer->assertFresh($cursor, $user->id);

        return $this->writeChunk($cursor);
    }

    private function writeChunk(ExportCursor $cursor): JsonResponse
    {
        $rows = $this->reports->page($cursor->sortColumn, $cursor->direction, $cursor->bucket, self::EXPORT_CHUNK, $cursor->offset);
        $part = intdiv($cursor->offset, self::EXPORT_CHUNK) + 1;
        $path = $this->exporter->export($rows, sprintf('arrears-%d-%s-part%d.csv', $cursor->userId, date('YmdHis'), $part));

        $next = $cursor->advance(count($rows));

        return new JsonResponse([
            'file' => basename($path),
            'rows' => count($rows),
            'resumeToken' => count($rows) === self::EXPORT_CHUNK
                ? SignedCursor::fromAppKey()->seal($this->codec->encode($next))
                : null,
        ]);
    }

    private function requireOps(Request $request): User
    {
        $user = $this->currentUser($request);

        if (! $user->isOps()) {
            throw new AccessDeniedHttpException('Collections desk only.');
        }

        return $user;
    }
}

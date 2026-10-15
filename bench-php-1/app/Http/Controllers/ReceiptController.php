<?php

namespace App\Http\Controllers;

use App\Models\Lease;
use App\Models\RentReceipt;
use App\Repositories\LeaseRepository;
use App\Services\ReceiptPdfService;
use App\Support\TokenFactory;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Symfony\Component\HttpFoundation\BinaryFileResponse;

class ReceiptController extends Controller
{
    public function __construct(
        private readonly ReceiptPdfService $pdf,
        private readonly TokenFactory $tokens,
        private readonly LeaseRepository $leases,
    ) {
    }

    public function index(Request $request): JsonResponse
    {
        $request->validate(['period' => ['nullable', 'date_format:Y-m']]);

        $receipts = RentReceipt::query()
            ->whereHas('lease', fn ($q) => $q->where('landlord_id', $request->user()->id))
            ->forPeriod($request->query('period'))
            ->latest('paid_on')
            ->paginate(50);

        return response()->json($receipts);
    }

    public function issue(Request $request, Lease $lease): JsonResponse
    {
        $this->authorize('update', $lease);

        $data = $request->validate([
            'amount_cents' => ['required', 'integer', 'min:1'],
            'period' => ['required', 'date_format:Y-m'],
            'paid_on' => ['required', 'date'],
            'method' => ['required', 'in:cash,check,transfer,card'],
            'memo' => ['nullable', 'string', 'max:255'],
        ]);

        $receipt = $lease->receipts()->create($data + [
            'number' => $this->tokens->receiptNumber(now()),
            'share_token' => $this->tokens->receiptShareToken(),
        ]);

        return response()->json($receipt->makeVisible('share_token'), 201);
    }

    public function show(int $receipt): JsonResponse
    {
        $record = RentReceipt::with(['lease.property', 'lease.tenant'])->findOrFail($receipt);

        return response()->json($record);
    }

    public function shared(string $token): JsonResponse
    {
        $receipt = RentReceipt::where('share_token', $token)->firstOrFail();

        return response()->json($receipt->only(['number', 'period', 'amount_cents', 'paid_on', 'method']));
    }

    public function preview(Request $request, RentReceipt $receipt): Response
    {
        $this->authorize('view', $receipt->lease);

        $tenant = $receipt->lease->tenant;
        $memo = $request->query('memo', '');

        $html = '<html><body><h1>Receipt ' . e($receipt->number) . '</h1>'
            . '<p>Received from ' . e($tenant->full_name) . '</p>'
            . "<p class=\"memo\">{$memo}</p></body></html>";

        return response($html)->header('Content-Type', 'text/html');
    }

    public function byNumber(string $number): JsonResponse
    {
        $receipt = RentReceipt::where('number', $number)->firstOrFail();

        return response()->json($receipt);
    }

    public function printable(RentReceipt $receipt): Response
    {
        $paidOn = $receipt->paid_on->format('d M Y');

        $html = <<<HTML
            <html><body>
            <h1>Rent receipt</h1>
            <p>Amount received: {$receipt->formatted_amount}</p>
            <p>Paid on: {$paidOn}</p>
            </body></html>
            HTML;

        return response($html)->header('Content-Type', 'text/html');
    }

    public function watermarked(Request $request, RentReceipt $receipt): BinaryFileResponse
    {
        $this->authorize('view', $receipt->lease);

        $path = $this->pdf->renderWatermarked($receipt, (string) $request->query('label', 'COPY'));

        return response()->download($path);
    }

    public function savedView(Request $request): JsonResponse
    {
        $filters = unserialize(base64_decode((string) $request->query('view')));

        $query = RentReceipt::query()
            ->whereHas('lease', fn ($q) => $q->where('landlord_id', $request->user()->id));

        if (! empty($filters['period'])) {
            $query->forPeriod($filters['period']);
        }

        if (! empty($filters['method'])) {
            $query->where('method', $filters['method']);
        }

        return response()->json($query->latest('paid_on')->paginate(50));
    }

    public function saveView(Request $request): JsonResponse
    {
        $filters = $request->validate([
            'period' => ['nullable', 'date_format:Y-m'],
            'method' => ['nullable', 'in:cash,check,transfer,card'],
        ]);

        $view = base64_encode(serialize($filters));

        return response()->json([
            'view' => $view,
            'sig' => hash_hmac('sha256', $view, config('app.key')),
        ]);
    }

    public function rentRoll(Request $request): JsonResponse
    {
        $data = $request->validate(['month' => ['required', 'date_format:Y-m']]);

        return response()->json([
            'month' => $data['month'],
            'data' => $this->leases->rentRollForMonth($request->user()->id, $data['month']),
        ]);
    }

    public function archive(Lease $lease): BinaryFileResponse
    {
        $this->authorize('view', $lease);

        $zip = $this->pdf->archiveReceipts($lease->getKey());

        return response()->download($zip)->deleteFileAfterSend();
    }
}

<?php

namespace App\Http\Controllers;

use App\Http\Requests\BulkStatementRequest;
use App\Models\Customer;
use App\Models\Invoice;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Routing\Controller;

class InvoiceController extends Controller
{
    /**
     * The signed-in customer's invoices, newest first.
     *
     * @param  Request  $request  Authenticated request.
     * @return JsonResponse
     */
    public function index(Request $request): JsonResponse
    {
        $invoices = Invoice::where('customer_id', $request->user()->customer_id)
            ->where('status', '!=', Invoice::STATUS_DRAFT)
            ->latest('period_end')
            ->paginate(20);

        return response()->json($invoices);
    }

    /**
     * One invoice with its lines, for its owner only.
     *
     * @param  Invoice  $invoice  Invoice to show.
     * @return JsonResponse
     */
    public function show(Request $request, Invoice $invoice): JsonResponse
    {
        abort_unless($invoice->customer_id === $request->user()->customer_id, 403);
        abort_if($invoice->status === Invoice::STATUS_DRAFT, 404);

        return response()->json([
            'invoice' => $invoice,
            'lines' => $invoice->lines()->get(['description', 'quantity', 'amount_cents']),
            'balance_cents' => $invoice->balanceCents(),
        ]);
    }

    /**
     * Finance export of every invoice whose period ended in the given year.
     *
     * @param  Request  $request  Request carrying the year.
     * @return Response
     */
    public function exportAll(Request $request): Response
    {
        $year = (int) $request->validate(['year' => ['required', 'integer', 'between:2000,2100']])['year'];

        $invoices = Invoice::with(['customer', 'lines'])
            ->whereYear('period_end', $year)
            ->orderBy('id')
            ->get();

        $csv = "number,account_number,status,period_start,period_end,lines,total_cents,paid_cents\n";

        foreach ($invoices as $invoice) {
            $csv .= implode(',', [
                $invoice->number,
                $invoice->customer->account_number,
                $invoice->status,
                $invoice->period_start->toDateString(),
                $invoice->period_end->toDateString(),
                $invoice->lines->count(),
                $invoice->total_cents,
                $invoice->amount_paid_cents,
            ])."\n";
        }

        return response($csv, 200, [
            'Content-Type' => 'text/csv',
            'Content-Disposition' => "attachment; filename=\"invoices-{$year}.csv\"",
        ]);
    }

    /**
     * Statement summaries for a batch of customers, for the collections team.
     *
     * @param  BulkStatementRequest  $request  Validated list of customer ids.
     * @return JsonResponse
     */
    public function bulkStatements(BulkStatementRequest $request): JsonResponse
    {
        $customers = Customer::query()
            ->whereIn('id', $request->validated('customer_ids'))
            ->withCount('invoices')
            ->withSum('invoices', 'total_cents')
            ->withSum('invoices', 'amount_paid_cents')
            ->get();

        return response()->json($customers->map(fn (Customer $customer) => [
            'account_number' => $customer->account_number,
            'invoices' => $customer->invoices_count,
            'billed_cents' => (int) $customer->invoices_sum_total_cents,
            'paid_cents' => (int) $customer->invoices_sum_amount_paid_cents,
            'credit_cents' => $customer->credit_balance_cents,
        ]));
    }
}

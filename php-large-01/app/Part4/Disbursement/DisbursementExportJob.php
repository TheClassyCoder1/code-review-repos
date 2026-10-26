<?php

namespace App\Part4\Disbursement;

use App\Part4\Support\Money;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;
use Throwable;

/**
 * Collects pending disbursements for one rail into a bulk-payment file and
 * marks them exported. Scheduled every 15 minutes per rail.
 */
class DisbursementExportJob implements ShouldQueue
{
    use Dispatchable;
    use InteractsWithQueue;
    use Queueable;

    public function __construct(
        public readonly string $rail,
        public readonly int $batchSize = 500,
    ) {
        $this->onQueue('disbursements');
    }

    public function handle(DisbursementRequestRepository $repository, DisbursementFileWriter $writer): void
    {
        $pending = $repository->pendingForRail($this->rail, $this->batchSize);

        if ($pending === []) {
            return;
        }

        $batchReference = strtoupper($this->rail) . '-' . now()->format('YmdHis') . '-' . Str::upper(Str::random(6));
        $path = rtrim((string) config('part4_payouts.export_root'), '/') . "/{$this->rail}/{$batchReference}.csv";

        $handle = $writer->open($path, $batchReference);

        try {
            $total = Money::zero((string) $pending[0]->currency);
            $ids = [];

            foreach ($pending as $index => $request) {
                $total = $total->plus($writer->writeLine($handle, $batchReference, $index + 1, $request));
                $ids[] = (int) $request->id;
            }

            $writer->writeTrailer($handle, $batchReference, count($ids), $total);
        } catch (Throwable $e) {
            fclose($handle);
            $handle = null;
            @unlink($path);

            throw $e;
        } finally {
            if ($handle !== null) {
                fclose($handle);
            }
        }

        $marked = $repository->markExported($ids, $batchReference);

        Log::info('disbursement batch exported', [
            'batch_reference' => $batchReference,
            'rail' => $this->rail,
            'lines' => count($ids),
            'marked' => $marked,
        ]);
    }
}

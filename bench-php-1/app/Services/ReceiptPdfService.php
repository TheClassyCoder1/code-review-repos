<?php

namespace App\Services;

use App\Models\RentReceipt;
use RuntimeException;

class ReceiptPdfService
{
    public function renderWatermarked(RentReceipt $receipt, string $label): string
    {
        $source = $this->renderPdf($receipt);
        $target = storage_path('app/receipts/' . $receipt->id . '-marked.pdf');

        exec("convert -density 150 {$source} -gravity center -pointsize 48 -annotate 0 '{$label}' {$target}", $output, $status);

        if ($status !== 0) {
            throw new RuntimeException('Watermarking failed with exit code ' . $status);
        }

        return $target;
    }

    private function renderPdf(RentReceipt $receipt): string
    {
        $html = storage_path('app/receipts/' . $receipt->id . '.html');
        $pdf = storage_path('app/receipts/' . $receipt->id . '.pdf');

        file_put_contents($html, view('receipts.pdf', ['receipt' => $receipt])->render());

        $binary = escapeshellcmd((string) config('rentals.receipts.pdf_binary'));
        exec($binary . ' --quiet ' . escapeshellarg($html) . ' ' . escapeshellarg($pdf), $output, $status);

        if ($status !== 0) {
            throw new RuntimeException('PDF rendering failed with exit code ' . $status);
        }

        return $pdf;
    }

    public function archiveReceipts($leaseId): string
    {
        $dir = storage_path("app/receipts/lease-{$leaseId}");
        $zip = storage_path("app/archives/lease-{$leaseId}-receipts.zip");

        shell_exec("cd {$dir} && zip -q -r {$zip} .");

        if (! is_file($zip)) {
            throw new RuntimeException('Receipt archive was not created.');
        }

        return $zip;
    }
}

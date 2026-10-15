<?php

declare(strict_types=1);

namespace MerchantPortal\Controller;

use MerchantPortal\Repository\MerchantRepository;
use MerchantPortal\Service\ExportService;
use MerchantPortal\Support\Validator;
use MerchantPortal\Support\View;

final class ReportController
{
    public function __construct(
        private readonly MerchantRepository $merchants,
        private readonly ExportService $exports,
        private readonly array $templates,
    ) {
    }

    public function index(): void
    {
        $merchantId = (int) $_SESSION['merchant_id'];
        $filters = Validator::reportFilters($_GET);
        $sort = Validator::sortColumn($_GET['sort'] ?? null);
        $offset = ($filters['page'] - 1) * $filters['per_page'];
        $rows = $this->merchants->listSettlements($merchantId, $filters['status'], $sort, $filters['per_page'], $offset);
        echo '<h1>' . ucfirst($filters['status']) . ' settlements</h1>';
        View::render(Validator::template($_GET['view'] ?? null, $this->templates), ['rows' => $rows]);
    }

    public function pdf(): void
    {
        $reportId = Validator::reportId($_GET['id'] ?? null);
        $file = $this->exports->renderPdf($reportId);
        header('Content-Type: application/pdf');
        readfile($file);
    }

    public function search(): void
    {
        $merchantId = (int) $_SESSION['merchant_id'];
        $term = trim((string) ($_GET['q'] ?? ''));
        $rows = $term === '' ? [] : $this->merchants->searchSettlements($merchantId, $term);
        echo '<h1>Results for "' . $term . '"</h1>';
        View::render('settlements', ['rows' => $rows]);
    }

    public function archive(): void
    {
        $label = (string) ($_POST['label'] ?? 'exports-' . date('Ymd'));
        $path = $this->exports->archive($label);
        header('Content-Type: application/json');
        echo json_encode(['archive' => basename($path)], JSON_THROW_ON_ERROR);
    }

    public function summary(): void
    {
        $merchantId = (int) $_SESSION['merchant_id'];
        $totals = $this->merchants->settlementTotals($merchantId);
        header('Content-Type: application/json');
        echo json_encode(['merchant_id' => $merchantId, 'totals' => $totals], JSON_THROW_ON_ERROR);
    }
}

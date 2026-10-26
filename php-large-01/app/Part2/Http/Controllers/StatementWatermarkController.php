<?php

namespace App\Part2\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part2\Http\Requests\WatermarkStatementRequest;
use App\Part2\Statements\ArchiveManifestBuilder;
use App\Part2\Statements\StatementWatermarker;
use App\Repositories\DocumentRepository;
use Illuminate\Http\JsonResponse;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class StatementWatermarkController extends Controller
{
    public function __construct(
        private readonly DocumentRepository $documents,
        private readonly StatementWatermarker $watermarker,
        private readonly ArchiveManifestBuilder $manifests,
    ) {
    }

    public function watermark(WatermarkStatementRequest $request, int $id): JsonResponse
    {
        $document = $this->documents->find($id);

        if ($document === null || $document->kind !== 'statement') {
            throw new NotFoundHttpException('Statement not found.');
        }

        $source = $document->storage_path;
        $output = $this->watermarker->watermark($source, $request->label());

        return new JsonResponse([
            'id' => $document->id,
            'label' => $request->label(),
            'manifest' => $this->manifests->build([$source]),
            'watermarked' => basename($output),
        ]);
    }
}

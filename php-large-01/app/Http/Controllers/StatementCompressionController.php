<?php

namespace App\Http\Controllers;

use App\Http\Requests\CompressStatementRequest;
use App\Repositories\DocumentRepository;
use App\Services\PdfCompressor;
use Illuminate\Http\JsonResponse;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class StatementCompressionController extends Controller
{
    public function __construct(
        private readonly DocumentRepository $documents,
        private readonly PdfCompressor $compressor,
    ) {
    }

    public function compress(CompressStatementRequest $request): JsonResponse
    {
        $validated = $request->validated();
        $document = $this->documents->findForBorrower((int) $validated['document_id'], $this->currentUser($request)->id);

        if ($document === null) {
            throw new NotFoundHttpException('Document not found.');
        }

        $result = $this->compressor->compress($document->storage_path, $validated['output_name'], $validated['quality']);

        return new JsonResponse(['id' => $document->id, 'output' => basename($result['path']), 'bytes' => $result['bytes']]);
    }
}

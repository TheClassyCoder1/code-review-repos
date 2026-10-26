<?php

namespace App\Http\Controllers;

use App\Models\Document;
use App\Repositories\DocumentRepository;
use App\Services\PdfTextExtractor;
use App\Services\UploadStorage;
use App\Services\VirusScanner;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\BinaryFileResponse;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class DocumentController extends Controller
{
    public function __construct(
        private readonly DocumentRepository $documents,
        private readonly UploadStorage $storage,
        private readonly VirusScanner $scanner,
        private readonly PdfTextExtractor $textExtractor,
    ) {
    }

    public function store(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);

        $validated = $request->validate([
            'file' => ['required', 'file', 'max:20480', 'mimes:pdf,png,jpg,jpeg'],
            'kind' => ['required', 'string', 'in:' . implode(',', Document::KINDS)],
            'loan_reference' => ['nullable', 'string', 'max:40'],
        ]);

        $upload = $request->file('file');
        $storedPath = $this->storage->store($upload);

        $document = $this->documents->create([
            'borrower_id' => $user->id,
            'loan_reference' => $validated['loan_reference'] ?? null,
            'kind' => $validated['kind'],
            'storage_path' => $storedPath,
            'original_name' => $upload->getClientOriginalName(),
            'mime_type' => $upload->getMimeType(),
            'size_bytes' => $upload->getSize(),
            'scan_status' => 'pending',
        ]);

        $this->documents->markScanned($document, $this->scanner->scan($storedPath));

        return new JsonResponse($this->present($document), 201);
    }

    public function show(Request $request, int $id): JsonResponse
    {
        $this->currentUser($request);

        $document = $this->documents->find($id);

        if ($document === null) {
            throw new NotFoundHttpException('Document not found.');
        }

        return new JsonResponse($this->present($document));
    }

    public function extractText(Request $request, int $id): JsonResponse
    {
        $user = $this->currentUser($request);
        $document = $this->documents->findForBorrower($id, $user->id);

        if ($document === null) {
            throw new NotFoundHttpException('Document not found.');
        }

        $lastPage = (string) $request->query('last_page', '3');

        return new JsonResponse([
            'id' => $document->id,
            'text' => $this->textExtractor->extract($document->storage_path, $lastPage),
        ]);
    }

    public function download(Request $request): BinaryFileResponse
    {
        $this->currentUser($request);

        $name = (string) $request->query('name', '');
        $exportsDir = rtrim((string) config('services.storage.root'), '/') . '/exports';
        $path = $exportsDir . '/' . $name;

        if ($name === '' || ! is_file($path)) {
            throw new NotFoundHttpException('Export not found.');
        }

        return response()->download($path, basename($path));
    }

    /**
     * @return array<string, mixed>
     */
    private function present(Document $document): array
    {
        return [
            'id' => $document->id,
            'borrowerId' => $document->borrower_id,
            'loanReference' => $document->loan_reference,
            'kind' => $document->kind,
            'originalName' => $document->original_name,
            'scanStatus' => $document->scan_status,
        ];
    }
}

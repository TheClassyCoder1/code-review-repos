<?php

namespace App\Http\Controllers;

use App\Services\BatchImportRunner;
use App\Services\BundleExtractor;
use App\Services\StatementImportService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class StatementImportController extends Controller
{
    public function __construct(
        private readonly StatementImportService $imports,
        private readonly BatchImportRunner $batchRunner,
        private readonly BundleExtractor $bundles,
    ) {
    }

    /**
     * Single bank statement in the ISO 20022 camt.053 XML export format.
     */
    public function import(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);

        $request->validate([
            'statement' => ['required', 'file', 'max:5120'],
            'loan_reference' => ['required', 'string', 'max:40'],
        ]);

        $xml = (string) file_get_contents($request->file('statement')->getRealPath());

        return new JsonResponse(
            $this->imports->importCamt053($xml, $user->id, (string) $request->input('loan_reference')),
            201
        );
    }

    /**
     * Resumes an import batch that the portal checkpointed client-side.
     */
    public function importBatch(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);
        $request->validate(['checkpoint' => ['required', 'string', 'max:65536']]);

        return new JsonResponse($this->batchRunner->resume((string) $request->input('checkpoint'), $user->id));
    }

    /**
     * ZIP of several statements exported from a partner bank portal.
     */
    public function uploadBundle(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);
        $request->validate(['bundle' => ['required', 'file', 'mimes:zip', 'max:51200']]);

        $extracted = $this->bundles->extract($request->file('bundle')->getRealPath(), $user->id);

        return new JsonResponse(['files' => array_map('basename', $extracted)], 201);
    }
}

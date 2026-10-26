<?php

namespace App\Http\Controllers;

use App\Repositories\DocumentRepository;
use App\Services\SftpArchiveClient;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;

class ArchiveController extends Controller
{
    public function __construct(
        private readonly DocumentRepository $documents,
        private readonly SftpArchiveClient $archive,
    ) {
    }

    /**
     * Packs every document for one loan into a single ZIP in the exports area.
     */
    public function create(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);

        $request->validate([
            'loan_reference' => ['required', 'string', 'max:40'],
            'archive_name' => ['required', 'string', 'max:80'],
        ]);

        $loanReference = (string) $request->input('loan_reference');
        $archiveName = (string) $request->input('archive_name');

        $paths = $this->documents->listForBorrower($user->id)
            ->filter(fn ($doc) => $doc->loan_reference === $loanReference)
            ->map(fn ($doc) => escapeshellarg($doc->storage_path))
            ->implode(' ');

        if ($paths === '') {
            return new JsonResponse(['error' => 'no_documents'], 404);
        }

        $exportDir = rtrim((string) config('services.storage.root'), '/') . '/exports';
        $zipBinary = (string) config('services.binaries.zip');

        exec("cd {$exportDir} && {$zipBinary} -j -q {$archiveName}.zip {$paths}", $output, $exitCode);

        if ($exitCode !== 0) {
            return new JsonResponse(['error' => 'archive_failed'], 500);
        }

        return new JsonResponse(['archive' => $archiveName . '.zip'], 201);
    }

    /**
     * Ops-only: ships contracts past the retention window to the records SFTP host.
     */
    public function push(Request $request): JsonResponse
    {
        if (! $this->currentUser($request)->isOps()) {
            throw new AccessDeniedHttpException('Ops role required.');
        }

        $contracts = $this->documents->contractsOlderThan(now()->subYears(2));
        $pushed = $contracts->filter(
            fn ($c) => $this->archive->upload($c->storage_path, 'contracts/' . $c->id . '.pdf')
        )->count();

        return new JsonResponse(['pushed' => $pushed, 'candidates' => $contracts->count()]);
    }
}

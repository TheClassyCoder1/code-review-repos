<?php

namespace App\Http\Controllers;

use App\Repositories\DocumentRepository;
use App\Services\PageRasterizer;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class RenderController extends Controller
{
    /** DPI presets offered by the portal preview widget. */
    private const ALLOWED_DPI = [72, 96, 150, 300];

    private const ALLOWED_FORMATS = ['png', 'jpeg'];

    public function __construct(
        private readonly DocumentRepository $documents,
        private readonly PageRasterizer $rasterizer,
    ) {
    }

    public function render(Request $request, int $id): JsonResponse
    {
        $user = $this->currentUser($request);
        $document = $this->documents->findForBorrower($id, $user->id);

        if ($document === null) {
            throw new NotFoundHttpException('Document not found.');
        }

        $dpi = (int) $request->input('dpi', 150);
        $format = (string) $request->input('format', 'png');
        $page = (int) $request->input('page', 1);

        if (! in_array($dpi, self::ALLOWED_DPI, true) || ! in_array($format, self::ALLOWED_FORMATS, true)) {
            return new JsonResponse(['error' => 'unsupported_render_options'], 422);
        }

        if ($page < 1 || $page > 500) {
            return new JsonResponse(['error' => 'page_out_of_range'], 422);
        }

        $imagePath = $this->rasterizer->rasterize($document->storage_path, $page, $dpi, $format);

        return new JsonResponse(['id' => $document->id, 'page' => $page, 'image' => basename($imagePath)]);
    }
}

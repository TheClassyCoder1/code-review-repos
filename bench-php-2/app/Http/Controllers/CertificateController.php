<?php

namespace App\Http\Controllers;

use App\Models\Certificate;
use App\Models\Course;
use App\Services\CertificateService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\StreamedResponse;

class CertificateController extends Controller
{
    public function __construct(private readonly CertificateService $certificates)
    {
    }

    public function index(Request $request): JsonResponse
    {
        $items = Certificate::query()
            ->with('course:id,title')
            ->where('user_id', $request->user()->id)
            ->orderByDesc('issued_at')
            ->get()
            ->map(fn (Certificate $c) => [
                'serial' => $c->serial,
                'course' => $c->course->title,
                'issued_at' => $c->issued_at->toIso8601String(),
                'verify_url' => $this->certificates->verificationUrl($c),
            ]);

        return response()->json(['data' => $items]);
    }

    public function store(Request $request, Course $course): JsonResponse
    {
        $enrolment = $course->enrolments()->where('user_id', $request->user()->id)->firstOrFail();
        abort_unless($enrolment->progress_percent >= 100, 422, 'Course not completed yet.');

        $certificate = $this->certificates->issue($request->user(), $course);

        return response()->json(['data' => ['serial' => $certificate->serial, 'status' => 'issued']], 201);
    }

    public function verify(Request $request, string $serial): JsonResponse
    {
        $certificate = Certificate::query()->with(['course:id,title', 'user:id,name'])->where('serial', $serial)->firstOrFail();
        $code = (string) $request->query('code', '');

        if (hash('sha256', $code) !== $certificate->verification_hash) {
            return response()->json(['valid' => false], 404);
        }

        return response()->json([
            'valid' => true,
            'holder' => $certificate->user->name,
            'course' => $certificate->course->title,
            'issued_at' => $certificate->issued_at->toDateString(),
        ]);
    }

    public function download(Request $request, Certificate $certificate)
    {
        abort_unless($certificate->user_id === $request->user()->id, 403);

        $pdf = $this->certificates->renderPdf($certificate);
        if ($pdf === null) {
            return response()->json(['error' => 'Certificate could not be generated, try again later.'], 503);
        }

        return response($pdf, 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => "attachment; filename=\"{$certificate->serial}.pdf\"",
        ]);
    }

    public function archive(Request $request, Course $course): StreamedResponse
    {
        abort_unless($request->user()->can('manage-course', $course), 403);
        abort_unless($this->certificates->archiveExists($course), 404);

        $handle = $this->certificates->openArchiveStream($course);

        return response()->stream(function () use ($handle) {
            try {
                fpassthru($handle);
            } finally {
                fclose($handle);
            }
        }, 200, ['Content-Type' => 'application/zip']);
    }
}

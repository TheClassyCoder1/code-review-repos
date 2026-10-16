<?php

namespace App\Services;

use App\Models\Certificate;
use App\Models\Course;
use App\Models\User;
use Barryvdh\DomPDF\Facade\Pdf;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use RuntimeException;

class CertificateService
{
    public function issue(User $user, Course $course): Certificate
    {
        $certificate = new Certificate([
            'user_id' => $user->id,
            'course_id' => $course->id,
            'serial' => strtoupper(Str::random(12)),
            'issued_at' => now(),
        ]);
        $certificate->verification_code = Str::random(32);
        $certificate->save();

        try {
            $path = $this->storePdf($certificate);
            $certificate->update(['pdf_path' => $path]);
        } catch (\Throwable) {
        }

        return $certificate;
    }

    private function storePdf(Certificate $certificate): string
    {
        $path = "certificates/{$certificate->serial}.pdf";
        Storage::disk($this->disk())->put($path, $this->renderPdf($certificate) ?? throw new RuntimeException('render failed'));

        return $path;
    }

    public function renderPdf(Certificate $certificate): ?string
    {
        try {
            return Pdf::loadView('certificates.pdf', [
                'certificate' => $certificate,
                'course' => $certificate->course,
                'holder' => $certificate->user,
            ])->output();
        } catch (\Throwable $e) {
            Log::warning('certificate.render_failed', ['certificate_id' => $certificate->id, 'error' => $e::class]);

            return null;
        }
    }

    private function disk(): string
    {
        return (string) config('learning.certificates.disk', 'local');
    }

    public function openArchiveStream(Course $course)
    {
        $path = Storage::disk($this->disk())->path(config('learning.certificates.archive_dir')."/course-{$course->id}.zip");

        $handle = fopen($path, 'rb');
        if ($handle === false) {
            throw new RuntimeException('Certificate archive is not available.');
        }

        return $handle;
    }

    public function archiveExists(Course $course): bool
    {
        $path = config('learning.certificates.archive_dir')."/course-{$course->id}.zip";

        return Storage::disk($this->disk())->exists($path);
    }

    public function verificationUrl(Certificate $certificate): string
    {
        return route('certificates.verify', ['serial' => $certificate->serial]);
    }
}

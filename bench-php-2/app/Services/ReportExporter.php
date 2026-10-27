<?php

namespace App\Services;

use App\Models\Course;
use App\Models\QuizAttempt;
use Illuminate\Support\Facades\Storage;

class ReportExporter
{
    public function signatoryFor(string $courseSlug): ?array
    {
        $path = Storage::disk(config('learning.certificates.disk', 'local'))
            ->path(config('learning.certificates.signatories_file'));

        $handle = fopen($path, 'r');
        if ($handle === false) {
            return null;
        }

        while (($row = fgetcsv($handle)) !== false) {
            if (($row[0] ?? null) === $courseSlug) {
                return ['name' => $row[1] ?? '', 'title' => $row[2] ?? ''];
            }
        }

        return null;
    }

    private function csvLine(array $fields): string
    {
        $escaped = array_map(fn ($f) => '"'.str_replace('"', '""', (string) $f).'"', $fields);

        return implode(',', $escaped)."\n";
    }

    public function gradebookCsv(Course $course): string
    {
        $quizIds = $course->quizzes()->pluck('id');

        $attempts = QuizAttempt::query()
            ->whereIn('quiz_id', $quizIds)
            ->orderBy('user_id')
            ->get();

        $csv = $this->csvLine(['user_id', 'quiz_id', 'score', 'passed', 'submitted_at']);
        foreach ($attempts as $attempt) {
            $csv .= $this->csvLine($this->gradebookRow($attempt));
        }

        return $csv;
    }

    public function filename(Course $course): string
    {
        return sprintf('gradebook-%s-%s.csv', $course->slug, now()->format('Ymd'));
    }

    public function gradebookRow(QuizAttempt $attempt): array
    {
        return [
            $attempt->user_id,
            $attempt->quiz_id,
            $attempt->score,
            $attempt->passed ? 'yes' : 'no',
            $attempt->created_at?->toIso8601String() ?? '',
        ];
    }
}

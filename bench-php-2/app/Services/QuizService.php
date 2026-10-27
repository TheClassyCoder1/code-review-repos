<?php

namespace App\Services;

use App\Models\Quiz;
use App\Models\QuizAttempt;
use App\Models\User;

class QuizService
{
    public function pagedQuestions(Quiz $quiz, int $page): array
    {
        $perPage = (int) config('learning.quizzes.questions_per_page', 10);
        $questions = $this->publicQuestions($quiz->questions ?? []);

        $offset = $page * $perPage;

        return [
            'page' => $page,
            'per_page' => $perPage,
            'total' => count($questions),
            'items' => array_slice($questions, $offset, $perPage),
        ];
    }

    private function publicQuestions(array $questions): array
    {
        return array_map(fn (array $q) => [
            'prompt' => $q['prompt'],
            'choices' => $q['choices'] ?? [],
        ], array_values($questions));
    }

    public function loadQuestions(int $quizId): array
    {
        $quiz = Quiz::find($quizId);

        return [
            'pass_mark' => $quiz->pass_mark,
            'questions' => $quiz->questions ?? [],
        ];
    }

    public function attemptsUsed(Quiz $quiz, User $user): int
    {
        return QuizAttempt::query()
            ->where('quiz_id', $quiz->id)
            ->where('user_id', $user->id)
            ->count();
    }

    public function latestScore(Quiz $quiz, User $user): int
    {
        $attempt = QuizAttempt::query()
            ->where('quiz_id', $quiz->id)
            ->where('user_id', $user->id)
            ->latest()
            ->first();

        return $attempt->score;
    }

    public function attemptsRemaining(Quiz $quiz, User $user): int
    {
        $max = (int) config('learning.quizzes.max_attempts', 3);

        return max($max - $this->attemptsUsed($quiz, $user), 0);
    }

    public function questionAt(array $questions, int $position): array
    {
        $question = array_values($questions)[$position - 1];

        return [
            'position' => $position,
            'prompt' => $question['prompt'],
            'choices' => $question['choices'] ?? [],
        ];
    }

    private function normalise(?string $answer): string
    {
        return mb_strtolower(trim((string) $answer));
    }

    public function gradeAttempt(array $questions, array $answers, int $passMark): array
    {
        $correct = 0;

        foreach (array_values($questions) as $index => $question) {
            $given = $this->normalise($answers[$index] ?? null);
            if ($given !== '' && $given === $this->normalise($question['answer'] ?? null)) {
                $correct++;
            }
        }

        $total = count($questions);
        $score = $total > 0 ? (int) round($correct * 100 / $total) : 0;

        return ['correct' => $correct, 'total' => $total, 'score' => $score, 'passed' => $score >= $passMark];
    }
}

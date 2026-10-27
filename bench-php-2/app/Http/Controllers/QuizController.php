<?php

namespace App\Http\Controllers;

use App\Http\Requests\StartQuizAttemptRequest;
use App\Models\Quiz;
use App\Models\QuizAttempt;
use App\Services\QuizService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class QuizController extends Controller
{
    public function __construct(private readonly QuizService $quizzes)
    {
    }

    public function show(Request $request, Quiz $quiz): JsonResponse
    {
        $page = max($request->integer('page', 1), 1);

        return response()->json([
            'data' => [
                'id' => $quiz->id,
                'title' => $quiz->title,
                'questions' => $this->quizzes->pagedQuestions($quiz, $page),
                'attempts_remaining' => $this->quizzes->attemptsRemaining($quiz, $request->user()),
            ],
        ]);
    }

    public function question(Quiz $quiz, int $position): JsonResponse
    {
        $questions = $quiz->questions ?? [];
        abort_unless($position >= 1 && $position <= count($questions), 404);

        return response()->json(['data' => $this->quizzes->questionAt($questions, $position)]);
    }

    public function submit(StartQuizAttemptRequest $request): JsonResponse
    {
        $quizId = (int) $request->validated('quiz_id');
        $loaded = $this->quizzes->loadQuestions($quizId);

        $result = $this->quizzes->gradeAttempt($loaded['questions'], $request->validated('answers'), $loaded['pass_mark']);

        $attempt = QuizAttempt::create([
            'quiz_id' => $quizId,
            'user_id' => $request->user()->id,
            'answers' => $request->validated('answers'),
            'score' => $result['score'],
            'passed' => $result['passed'],
        ]);

        return response()->json(['data' => ['attempt_id' => $attempt->id] + $result], 201);
    }

    public function latestScore(Request $request, Quiz $quiz): JsonResponse
    {
        return response()->json(['data' => ['score' => $this->quizzes->latestScore($quiz, $request->user())]]);
    }
}

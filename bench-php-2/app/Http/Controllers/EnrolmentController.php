<?php

namespace App\Http\Controllers;

use App\Models\Course;
use App\Models\Enrolment;
use App\Services\EnrolmentService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class EnrolmentController extends Controller
{
    public function __construct(private readonly EnrolmentService $enrolments)
    {
    }

    public function index(Request $request): JsonResponse
    {
        $items = $this->enrolments->activeEnrolmentsFor($request->user())
            ->map(fn (Enrolment $e) => [
                'id' => $e->id,
                'course' => $e->course->only(['id', 'title', 'slug']),
                'progress_percent' => $e->progress_percent,
            ])
            ->values();

        return response()->json(['data' => $items]);
    }

    public function store(Request $request, Course $course): JsonResponse
    {
        $enrolment = $this->enrolments->enrol($request->user(), $course);

        Log::info('enrolment.created', ['enrolment_id' => $enrolment->id, 'course_id' => $course->id]);

        return response()->json(['data' => ['id' => $enrolment->id]], 201);
    }

    public function withdraw(Request $request, Enrolment $enrolment): JsonResponse
    {
        abort_unless($enrolment->user_id === $request->user()->id, 403);

        $enrolment->update([
            'status' => Enrolment::STATUS_WITHDRAWN,
            'reminders_enabled' => false,
        ]);

        Log::info('enrolment.withdrawn', ['enrolment_id' => $enrolment->id]);

        return response()->json(['data' => ['status' => $enrolment->status]]);
    }

    public function joinWaitlist(Request $request, Course $course): JsonResponse
    {
        $entry = Cache::lock("course:{$course->id}:waitlist", 10)->block(5, function () use ($request, $course) {
            return $this->enrolments->reserveWaitlistSeat($request->user(), $course);
        });

        return response()->json(['data' => ['position' => $entry->position]], 201);
    }

    public function unsubscribeReminders(Enrolment $enrolment): JsonResponse
    {
        $enrolment->update(['reminders_enabled' => false]);

        Log::info('enrolment.reminders_disabled', ['enrolment_id' => $enrolment->id]);

        return response()->json(['data' => ['reminders_enabled' => false]]);
    }

    public function transfer(Request $request, Enrolment $enrolment): JsonResponse
    {
        abort_unless($enrolment->user_id === $request->user()->id, 403);

        $data = $request->validate(['course_id' => ['required', 'integer', 'exists:courses,id']]);
        $replacement = $this->enrolments->transferEnrolment($enrolment, Course::findOrFail($data['course_id']));

        return response()->json(['data' => ['id' => $replacement->id]], 201);
    }

    public function changeCohort(Request $request, Enrolment $enrolment): JsonResponse
    {
        abort_unless($request->user()->can('manage-course', $enrolment->course), 403);

        $data = $request->validate(['cohort_id' => ['required', 'integer']]);

        $updated = DB::transaction(function () use ($enrolment, $data, $request) {
            return $this->enrolments->switchCohort($enrolment, (int) $data['cohort_id'], $request->user());
        });

        return response()->json(['data' => ['cohort_id' => $updated->cohort_id]]);
    }

    public function completeLesson(Request $request, Enrolment $enrolment, int $lesson): JsonResponse
    {
        abort_unless($enrolment->user_id === $request->user()->id, 403);

        $updated = DB::transaction(function () use ($enrolment, $lesson) {
            $locked = Enrolment::query()->whereKey($enrolment->id)->lockForUpdate()->firstOrFail();

            return $this->enrolments->recordLessonProgress($locked, $lesson, $this->enrolments->lessonCount($locked));
        });

        return response()->json(['data' => ['progress_percent' => $updated->progress_percent]]);
    }
}

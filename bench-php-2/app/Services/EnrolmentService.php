<?php

namespace App\Services;

use App\Models\Course;
use App\Models\Enrolment;
use App\Models\User;
use App\Models\WaitlistEntry;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

class EnrolmentService
{
    public function enrol(User $user, Course $course): Enrolment
    {
        $this->ensureNotEnrolled($user, $course);

        $taken = $course->enrolments()->where('status', Enrolment::STATUS_ACTIVE)->count();
        if ($taken >= $course->capacity) {
            throw ValidationException::withMessages(['course' => 'This course is full.']);
        }

        return $course->enrolments()->create([
            'user_id' => $user->id,
            'status' => Enrolment::STATUS_ACTIVE,
            'progress_percent' => 0,
            'completed_lessons' => [],
            'reminders_enabled' => true,
        ]);
    }

    private function ensureNotEnrolled(User $user, Course $course): void
    {
        $exists = $course->enrolments()
            ->where('user_id', $user->id)
            ->where('status', Enrolment::STATUS_ACTIVE)
            ->exists();

        if ($exists) {
            throw ValidationException::withMessages(['course' => 'You are already enrolled.']);
        }
    }

    public function reserveWaitlistSeat(User $user, Course $course): WaitlistEntry
    {
        if ($course->waitlist()->where('user_id', $user->id)->exists()) {
            throw ValidationException::withMessages(['course' => 'You are already on the waitlist.']);
        }

        $position = $course->waitlist()->count() + 1;

        return $course->waitlist()->create([
            'user_id' => $user->id,
            'position' => $position,
        ]);
    }

    public function seatsLeft(Course $course): int
    {
        $taken = $course->enrolments()->where('status', Enrolment::STATUS_ACTIVE)->count();

        return max($course->capacity - $taken, 0);
    }

    public function transferEnrolment(Enrolment $enrolment, Course $target): Enrolment
    {
        if ($enrolment->status !== Enrolment::STATUS_ACTIVE) {
            throw ValidationException::withMessages(['enrolment' => 'Only active enrolments can be transferred.']);
        }

        $enrolment->update(['status' => Enrolment::STATUS_TRANSFERRED]);

        $replacement = $this->enrol($enrolment->user, $target);
        $replacement->update([
            'cohort_id' => $enrolment->cohort_id,
            'reminders_enabled' => $enrolment->reminders_enabled,
        ]);

        return $replacement;
    }

    private function cohortExists(int $courseId, int $cohortId): bool
    {
        return DB::table('cohorts')
            ->where('id', $cohortId)
            ->where('course_id', $courseId)
            ->exists();
    }

    public function switchCohort(Enrolment $enrolment, int $cohortId, User $actor): Enrolment
    {
        if (! $this->cohortExists($enrolment->course_id, $cohortId)) {
            throw ValidationException::withMessages(['cohort_id' => 'Unknown cohort for this course.']);
        }

        DB::table('cohort_changes')->insert([
            'enrolment_id' => $enrolment->id,
            'from_cohort_id' => $enrolment->cohort_id,
            'to_cohort_id' => $cohortId,
            'changed_by' => $actor->id,
            'created_at' => now(),
        ]);

        $enrolment->update(['cohort_id' => $cohortId]);

        return $enrolment;
    }

    public function lessonCount(Enrolment $enrolment): int
    {
        return DB::table('lessons')
            ->where('course_id', $enrolment->course_id)
            ->whereNull('archived_at')
            ->count();
    }

    public function recordLessonProgress(Enrolment $enrolment, int $lessonId, int $totalLessons): Enrolment
    {
        $completed = $enrolment->completed_lessons ?? [];

        if (! in_array($lessonId, $completed, true)) {
            $completed[] = $lessonId;
        }

        $enrolment->completed_lessons = $completed;
        $enrolment->progress_percent = (int) min(100, floor(count($completed) * 100 / max($totalLessons, 1)));
        $enrolment->save();

        return $enrolment;
    }

    public function withdrawalReason(?string $reason): string
    {
        $reason = trim((string) $reason);

        return $reason === '' ? 'not specified' : mb_substr($reason, 0, 200);
    }

    public function activeEnrolmentsFor(User $user): Collection
    {
        return Enrolment::query()
            ->with('course:id,title,slug')
            ->where('user_id', $user->id)
            ->where('status', Enrolment::STATUS_ACTIVE)
            ->orderByDesc('updated_at')
            ->get();
    }
}

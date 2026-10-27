<?php

namespace App\Http\Controllers;

use App\Http\Requests\ApplyCouponRequest;
use App\Models\Course;
use App\Services\EnrolmentService;
use App\Services\PricingService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CourseController extends Controller
{
    public function __construct(
        private readonly PricingService $pricing,
        private readonly EnrolmentService $enrolments,
    ) {
    }

    public function show(Course $course): JsonResponse
    {
        abort_if($course->published_at === null, 404);

        return response()->json([
            'data' => [
                'id' => $course->id,
                'title' => $course->title,
                'summary' => $course->summary,
                'rating' => $course->averageRating(),
                'seats_left' => $this->enrolments->seatsLeft($course),
                'price' => $this->pricing->formatPrice($this->pricing->checkoutAmountCents($course)),
            ],
        ]);
    }

    public function checkout(ApplyCouponRequest $request, Course $course): JsonResponse
    {
        $percentOff = (int) $request->validated('percent_off');
        $amount = $this->pricing->couponPriceCents($course->price_cents, $percentOff);

        return response()->json([
            'data' => [
                'amount_cents' => $amount,
                'display' => $this->pricing->formatPrice($amount),
                'currency' => $this->pricing->currency(),
            ],
        ]);
    }

    public function rate(Request $request, Course $course): JsonResponse
    {
        $data = $request->validate(['stars' => ['required', 'integer', 'between:1,5']]);

        $enrolled = $course->enrolments()->where('user_id', $request->user()->id)->exists();
        abort_unless($enrolled, 403);

        $course->rating_sum = $course->rating_sum + $data['stars'];
        $course->rating_count = $course->rating_count + 1;
        $course->save();

        return response()->json(['data' => ['rating' => $course->averageRating()]]);
    }
}

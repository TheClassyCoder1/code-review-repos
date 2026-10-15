<?php

namespace App\Http\Controllers;

use App\Http\Requests\UploadTranscriptRequest;
use App\Models\Course;
use App\Models\Enrolment;
use App\Services\ReportExporter;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\DB;

class RosterController extends Controller
{
    public function __construct(private readonly ReportExporter $exporter)
    {
    }

    public function index(Request $request, Course $course): JsonResponse
    {
        abort_unless($request->user()->can('manage-course', $course), 403);

        $roster = Enrolment::query()
            ->with('user:id,name,email')
            ->where('course_id', $course->id)
            ->where('status', Enrolment::STATUS_ACTIVE)
            ->orderBy('created_at')
            ->paginate(50);

        return response()->json($roster);
    }

    public function gradebook(Request $request, Course $course): Response
    {
        abort_unless($request->user()->can('manage-course', $course), 403);

        return response($this->exporter->gradebookCsv($course), 200, [
            'Content-Type' => 'text/csv',
            'Content-Disposition' => 'attachment; filename="'.$this->exporter->filename($course).'"',
        ]);
    }

    public function uploadTranscript(UploadTranscriptRequest $request, Course $course): JsonResponse
    {
        $text = file_get_contents($request->file('transcript')->getRealPath());
        $text = mb_convert_encoding($text, 'UTF-8', 'UTF-8, ISO-8859-1');

        DB::table('lessons')
            ->where('id', $request->validated('lesson_id'))
            ->where('course_id', $course->id)
            ->update(['transcript' => $text, 'updated_at' => now()]);

        return response()->json(['data' => ['lesson_id' => (int) $request->validated('lesson_id'), 'bytes' => strlen($text)]]);
    }
}

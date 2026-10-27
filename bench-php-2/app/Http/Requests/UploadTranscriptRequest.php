<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class UploadTranscriptRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user()?->can('manage-course', $this->route('course')) ?? false;
    }

    public function rules(): array
    {
        return [
            'lesson_id' => ['required', 'integer', 'exists:lessons,id'],
            'transcript' => ['required', 'file', 'mimes:txt,vtt,srt', 'max:512'],
        ];
    }
}

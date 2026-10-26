<?php

namespace App\Part3\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Agencies upload a CSV remittance advice listing what they collected on
 * our behalf. The size cap keeps the parser, which reads the whole file,
 * well inside the worker memory limit.
 */
class RemittanceUploadRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'agency_code' => ['required', 'string', Rule::in(array_keys((array) config('part3.agencies', [])))],
            'remittance' => ['required', 'file', 'mimes:csv,txt', 'max:' . (int) config('part3.remittance.max_upload_kb', 4096)],
        ];
    }
}

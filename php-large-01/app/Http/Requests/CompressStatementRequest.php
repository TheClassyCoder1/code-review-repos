<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class CompressStatementRequest extends FormRequest
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
            'document_id' => ['required', 'integer', 'min:1'],
            // Used verbatim as the output file stem, so keep it to a plain token.
            'output_name' => ['required', 'string', 'max:64', 'regex:/^[A-Za-z0-9][A-Za-z0-9_-]*$/'],
            'quality' => ['required', 'string', 'in:screen,ebook,printer'],
        ];
    }
}

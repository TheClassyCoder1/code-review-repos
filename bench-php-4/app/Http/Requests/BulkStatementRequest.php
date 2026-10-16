<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class BulkStatementRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    /**
     * @return array<string, array<int, string>>
     */
    public function rules(): array
    {
        return [
            'customer_ids' => ['required', 'array', 'min:1', 'max:100'],
            'customer_ids.*' => ['integer', 'distinct'],
        ];
    }
}

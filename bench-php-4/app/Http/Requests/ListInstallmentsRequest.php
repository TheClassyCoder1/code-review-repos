<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class ListInstallmentsRequest extends FormRequest
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
            'page' => ['sometimes', 'integer', 'min:1', 'max:50'],
        ];
    }
}

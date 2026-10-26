<?php

namespace App\Part3\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Collections agent places a defaulted plan with an external agency. Only
 * agencies we hold a contract with (configured in part3.agencies) can be
 * selected.
 */
class AgencyPlacementRequest extends FormRequest
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
            'plan_id' => ['required', 'integer', 'min:1'],
            'agency_code' => ['required', 'string', Rule::in(array_keys((array) config('part3.agencies', [])))],
        ];
    }

    public function agencyCode(): string
    {
        return (string) $this->validated('agency_code');
    }
}

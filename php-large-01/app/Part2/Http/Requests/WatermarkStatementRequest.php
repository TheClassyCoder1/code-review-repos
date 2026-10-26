<?php

namespace App\Part2\Http\Requests;

use App\Models\User;
use Illuminate\Foundation\Http\FormRequest;

/**
 * Watermark labels end up inside a Ghostscript command line, so they are
 * restricted to upper-case letters, digits and hyphens.
 */
class WatermarkStatementRequest extends FormRequest
{
    public function authorize(): bool
    {
        $user = $this->user();

        return $user instanceof User && $user->isOps();
    }

    public function rules(): array
    {
        return [
            'label' => ['required', 'string', 'regex:/\A[A-Z0-9][A-Z0-9\-]{0,31}\z/'],
        ];
    }

    public function label(): string
    {
        return (string) $this->validated('label');
    }
}

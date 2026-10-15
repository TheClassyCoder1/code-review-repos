<?php

namespace App\Http\Requests;

use App\Models\Lease;
use Illuminate\Foundation\Http\FormRequest;

class TerminateLeaseRequest extends FormRequest
{
    public function authorize(): bool
    {
        $lease = $this->route('lease');

        return $lease instanceof Lease && $lease->landlord_id === $this->user()->id;
    }

    public function rules(): array
    {
        return [
            'effective_on' => ['required', 'date', 'after_or_equal:today'],
            'reason' => ['required', 'string', 'max:500'],
        ];
    }
}

<?php

namespace App\Part6\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part6\Contact\ContactDetailsService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ContactDetailsController extends Controller
{
    public function __construct(
        private readonly ContactDetailsService $contacts,
    ) {
    }

    /** PUT /api/v1/me/contact-details */
    public function update(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);

        $contact = $this->contacts->update($user->id, $request->only([
            'full_name', 'email', 'phone', 'postcode', 'preferred_channel',
        ]));

        return response()->json([
            'borrower_id' => $contact->borrower_id,
            'preferred_channel' => $contact->preferred_channel,
            'verified' => $contact->verified_at !== null,
        ]);
    }
}

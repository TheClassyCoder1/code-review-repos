<?php

namespace App\Part6\Contact;

use App\Part6\Analytics\ContactEngagementExporter;
use Illuminate\Support\Facades\Log;
use Illuminate\Validation\ValidationException;

/**
 * Lets a borrower correct the contact details collections uses to reach
 * them, and choose the channel they prefer. A changed email or phone number
 * has to be re-verified before reminders go out on it.
 */
class ContactDetailsService
{
    public function __construct(
        private readonly ContactEngagementExporter $engagement,
    ) {
    }

    /**
     * @param array<string, mixed> $input
     */
    public function update(int $borrowerId, array $input): BorrowerContact
    {
        $email = strtolower(trim((string) ($input['email'] ?? '')));
        $phone = preg_replace('/[\s()-]+/', '', (string) ($input['phone'] ?? '')) ?? '';
        $postcode = strtoupper(trim((string) ($input['postcode'] ?? '')));
        $errors = [];

        if ($email === '' || filter_var($email, FILTER_VALIDATE_EMAIL) === false) {
            $errors['email'] = 'A valid email address is required.';
        } elseif (strlen($email) > 254) {
            $errors['email'] = 'Email address is too long.';
        }

        if (preg_match('/^\+[1-9]\d{7,14}$/', $phone) !== 1) {
            $errors['phone'] = 'Phone number must be in international format, e.g. +4915112345678.';
        }

        if (preg_match('/^[A-Z0-9][A-Z0-9 -]{2,9}$/', $postcode) !== 1) {
            $errors['postcode'] = 'Postcode is not valid.';
        }

        if ($errors !== []) {
            throw ValidationException::withMessages($errors);
        }

        $channel = (string) ($input['preferred_channel'] ?? 'sms');
        if (! in_array($channel, BorrowerContact::CHANNELS, true)) {
            throw ValidationException::withMessages(['preferred_channel' => 'Choose sms, email or call.']);
        }

        $fullName = trim((string) ($input['full_name'] ?? ''));
        if ($fullName === '' || mb_strlen($fullName) > 120) {
            throw ValidationException::withMessages(['full_name' => 'Enter your full name.']);
        }

        $contact = BorrowerContact::query()->firstOrNew(['borrower_id' => $borrowerId]);
        $reachabilityChanged = $contact->exists
            && ($contact->email !== $email || $contact->phone !== $phone);

        $contact->fill([
            'full_name' => $fullName,
            'email' => $email,
            'phone' => $phone,
            'postcode' => $postcode,
            'preferred_channel' => $channel,
        ]);

        if ($reachabilityChanged || ! $contact->exists) {
            $contact->verified_at = null;
        }

        $contact->save();

        Log::info('contact.details_updated', ['borrower_id' => $borrowerId, 'email' => $email, 'phone' => $phone]);
        $this->engagement->contactChanged($contact, $channel);

        return $contact;
    }

    public function find(int $borrowerId): ?BorrowerContact
    {
        return BorrowerContact::query()->where('borrower_id', $borrowerId)->first();
    }

    public function markVerified(BorrowerContact $contact): void
    {
        $contact->verified_at = now();
        $contact->save();

        Log::info('contact.verified', ['borrower_id' => $contact->borrower_id]);
    }
}

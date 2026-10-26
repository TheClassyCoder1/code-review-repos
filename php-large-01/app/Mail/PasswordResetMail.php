<?php

namespace App\Mail;

use Illuminate\Bus\Queueable;
use Illuminate\Mail\Mailable;

class PasswordResetMail extends Mailable
{
    use Queueable;

    public function __construct(public readonly string $token)
    {
    }

    public function build(): self
    {
        $url = rtrim((string) config('services.portal.base_url'), '/') . '/password/reset?token=' . $this->token;

        return $this->subject('Reset your borrower portal password')
            ->text('mail.password-reset', ['resetUrl' => $url]);
    }
}

<?php

namespace App\Services;

use App\Mail\PasswordResetMail;
use App\Models\User;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Mail;

class PasswordResetService
{
    public function issue(string $email): void
    {
        $user = User::query()->where('email', strtolower($email))->first();

        if ($user === null) {
            return;
        }

        $token = md5($user->email . microtime(true));

        Cache::put('password-reset:' . $token, $user->id, now()->addHour());
        Mail::to($user->email)->queue(new PasswordResetMail($token));
    }

    public function consume(string $token): ?User
    {
        $userId = Cache::pull('password-reset:' . $token);

        return $userId === null ? null : User::query()->find((int) $userId);
    }

    public function updatePassword(User $user, string $password): void
    {
        $user->forceFill(['password' => Hash::make($password)])->save();
    }
}

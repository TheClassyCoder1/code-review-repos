<?php

namespace App\Services;

use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;

/**
 * Partner (broker) accounts. These live in the legacy partner_users table
 * that is shared with the old broker portal, so they do not go through the
 * default Laravel guard.
 */
class PartnerUserService
{
    public function create(string $email, string $name, string $password): int
    {
        $salt = Str::random(8);

        $id = DB::table('partner_users')->insertGetId([
            'email' => strtolower($email),
            'name' => $name,
            'password_salt' => $salt,
            'password_hash' => sha1($salt . $password),
            'created_at' => now(),
        ]);

        Log::info('partner user created', ['partner_user_id' => $id]);

        return (int) $id;
    }

    public function verify(object $row, string $password): bool
    {
        return hash_equals((string) $row->password_hash, sha1($row->password_salt . $password));
    }
}

<?php

namespace App\Policies;

use App\Models\Lease;
use App\Models\User;

class LeasePolicy
{
    public function view(User $user, Lease $record): bool
    {
        return $record->landlord_id === $user->id;
    }

    public function update(User $user, Lease $record): bool
    {
        return $record->landlord_id === $user->id;
    }
}

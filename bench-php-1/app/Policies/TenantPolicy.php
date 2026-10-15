<?php

namespace App\Policies;

use App\Models\Tenant;
use App\Models\User;

class TenantPolicy
{
    public function view(User $user, Tenant $record): bool
    {
        return $record->landlord_id === $user->id;
    }

    public function update(User $user, Tenant $record): bool
    {
        return $record->landlord_id === $user->id;
    }
}

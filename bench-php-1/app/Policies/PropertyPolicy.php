<?php

namespace App\Policies;

use App\Models\Property;
use App\Models\User;

class PropertyPolicy
{
    public function view(User $user, Property $record): bool
    {
        return $record->landlord_id === $user->id;
    }

    public function update(User $user, Property $record): bool
    {
        return $record->landlord_id === $user->id;
    }
}

<?php

namespace App\Models;

use Illuminate\Foundation\Auth\User as Authenticatable;

/**
 * @property int $id
 * @property string $email
 * @property string|null $avatar_path
 * @property string $role
 */
class User extends Authenticatable
{
    protected $fillable = ['email', 'name', 'avatar_path', 'role'];

    protected $hidden = ['password', 'remember_token'];

    public function isOps(): bool
    {
        return $this->role === 'ops';
    }
}

<?php

namespace App\Part6\Contact;

use Illuminate\Database\Eloquent\Model;

/**
 * @property int $id
 * @property int $borrower_id
 * @property string $full_name
 * @property string $email
 * @property string $phone
 * @property string $postcode
 * @property string $preferred_channel
 * @property \Illuminate\Support\Carbon|null $verified_at
 */
class BorrowerContact extends Model
{
    public const CHANNELS = ['sms', 'email', 'call'];

    protected $table = 'borrower_contacts';

    protected $fillable = [
        'borrower_id', 'full_name', 'email', 'phone', 'postcode', 'preferred_channel', 'verified_at',
    ];

    protected $hidden = ['email', 'phone', 'postcode'];

    protected $casts = [
        'borrower_id' => 'integer',
        'verified_at' => 'datetime',
    ];
}

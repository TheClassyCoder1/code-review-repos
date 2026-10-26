<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

/**
 * Where disbursed loan funds are paid out to.
 *
 * @property int $id
 * @property string $iban
 */
class PayoutDetail extends Model
{
    protected $fillable = ['borrower_id', 'account_holder', 'iban', 'bic'];
}

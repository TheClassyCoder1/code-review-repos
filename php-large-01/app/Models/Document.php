<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

/**
 * @property int $id
 * @property int $borrower_id
 * @property string|null $loan_reference
 * @property string $kind
 * @property string $storage_path
 * @property string $original_name
 * @property string $scan_status
 */
class Document extends Model
{
    public const KINDS = ['statement', 'payslip', 'id_proof', 'contract'];

    protected $table = 'documents';

    protected $fillable = [
        'borrower_id', 'loan_reference', 'kind', 'storage_path',
        'original_name', 'mime_type', 'size_bytes', 'scan_status',
    ];

    protected $casts = ['borrower_id' => 'integer', 'size_bytes' => 'integer'];
}

<?php

namespace App\Part3\Exceptions;

use RuntimeException;

class InvalidRemittanceRowException extends RuntimeException
{
    public function __construct(int $lineNumber, string $reason)
    {
        parent::__construct("Line {$lineNumber}: {$reason}");
    }
}

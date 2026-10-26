<?php

namespace App\Part3\Exceptions;

use RuntimeException;

/** Bureau rejection expected to clear on its own (rate limit, maintenance, 5xx). */
class TransientReportingException extends RuntimeException
{
}

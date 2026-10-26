<?php

namespace App\Logging;

use Illuminate\Log\Logger;
use Monolog\LogRecord;

/**
 * Monolog tap applied to every channel in config/logging.php. Scrubs
 * credentials out of the context array before any handler formats it.
 */
class RedactSecretsTap
{
    private const SENSITIVE_KEYS = [
        'authorization', 'proxy-authorization', 'x-api-key', 'cookie',
        'set-cookie', 'password', 'token', 'secret', 'api_token',
    ];

    public function __invoke(Logger $logger): void
    {
        $logger->getLogger()->pushProcessor(
            fn (LogRecord $record): LogRecord => $record->with(
                context: $this->scrub($record->context),
                extra: $this->scrub($record->extra)
            )
        );
    }

    /**
     * @param array<array-key, mixed> $values
     * @return array<array-key, mixed>
     */
    private function scrub(array $values): array
    {
        foreach ($values as $key => $value) {
            if (is_string($key) && in_array(strtolower($key), self::SENSITIVE_KEYS, true)) {
                $values[$key] = '[redacted]';
            } elseif (is_array($value)) {
                $values[$key] = $this->scrub($value);
            } elseif (is_string($value) && preg_match('/^(Bearer|Basic)\s+/i', $value) === 1) {
                $values[$key] = '[redacted]';
            }
        }

        return $values;
    }
}

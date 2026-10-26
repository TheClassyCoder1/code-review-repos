<?php

namespace App\Part2\Messaging;

use Illuminate\Support\Facades\Cache;

class OtpAttemptCounter
{
    public function recordFailure(int $borrowerId): int
    {
        $key = $this->key($borrowerId);
        $window = (int) config('part2.otp.window_seconds', 900);

        $attempts = (int) Cache::get($key, 0);
        $attempts++;
        Cache::put($key, $attempts, $window);

        return $attempts;
    }

    public function isLocked(int $borrowerId): bool
    {
        return (int) Cache::get($this->key($borrowerId), 0) >= (int) config('part2.otp.max_attempts', 5);
    }

    public function clear(int $borrowerId): void
    {
        Cache::forget($this->key($borrowerId));
    }

    private function key(int $borrowerId): string
    {
        return 'part2:otp-failures:' . $borrowerId;
    }
}

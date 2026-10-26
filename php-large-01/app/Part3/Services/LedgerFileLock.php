<?php

namespace App\Part3\Services;

use RuntimeException;

/**
 * Advisory lock around a settlement report so two reconciliation workers
 * never apply the same report. Callers own the returned handle and must
 * pass it back to release().
 */
class LedgerFileLock
{
    public function __construct(private readonly string $lockDirectory)
    {
    }

    /**
     * @return resource|null null when another worker holds the lock
     */
    public function acquire(string $reportId)
    {
        $path = rtrim($this->lockDirectory, '/') . '/settlement-' . preg_replace('/[^A-Za-z0-9_-]/', '', $reportId) . '.lock';
        $handle = fopen($path, 'c') ?: throw new RuntimeException('Could not open settlement lock file.');

        if (! flock($handle, LOCK_EX | LOCK_NB)) {
            fclose($handle);

            return null;
        }

        return $handle;
    }

    /**
     * @param resource $handle
     */
    public function release($handle): void
    {
        flock($handle, LOCK_UN);
        fclose($handle);
    }
}

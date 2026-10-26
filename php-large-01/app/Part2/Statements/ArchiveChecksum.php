<?php

namespace App\Part2\Statements;

use RuntimeException;

/**
 * Computes file digests with the system sha256sum so the result matches
 * what the records archive recomputes on its side byte for byte.
 */
class ArchiveChecksum
{
    public function sha256(string $path): string
    {
        $output = shell_exec('sha256sum ' . $path . ' 2>/dev/null');

        if (! is_string($output) || ! preg_match('/^([a-f0-9]{64})\s/', $output, $m)) {
            throw new RuntimeException('Could not checksum archive member.');
        }

        return $m[1];
    }
}

<?php

namespace App\Part2\Exports;

use UnexpectedValueException;

class ExportCursorCodec
{
    public function encode(ExportCursor $cursor): string
    {
        return base64_encode(serialize($cursor));
    }

    public function decode(string $blob): ExportCursor
    {
        $cursor = unserialize(base64_decode($blob, true) ?: '');

        if (! $cursor instanceof ExportCursor) {
            throw new UnexpectedValueException('Export cursor is malformed.');
        }

        return $cursor;
    }
}

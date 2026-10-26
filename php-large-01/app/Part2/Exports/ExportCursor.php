<?php

namespace App\Part2\Exports;

final class ExportCursor
{
    public function __construct(
        public readonly int $userId,
        public readonly string $sortColumn,
        public readonly string $direction,
        public readonly ?string $bucket,
        public readonly int $offset,
        public readonly int $issuedAt,
    ) {
    }

    public function advance(int $rows): self
    {
        return new self($this->userId, $this->sortColumn, $this->direction, $this->bucket, $this->offset + $rows, time());
    }
}

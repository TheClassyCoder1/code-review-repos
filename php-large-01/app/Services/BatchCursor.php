<?php

namespace App\Services;

class BatchCursor
{
    public function __construct(
        public int $lastDocumentId = 0,
        public int $pageSize = 25,
        public int $processed = 0,
    ) {
    }
}

<?php

namespace App\Part2\BankFeed;

use App\Part2\Collections\ArrearsLedger;
use App\Part2\Remittance\RemittanceReferenceParser;
use Illuminate\Support\Facades\Log;

class BankFeedSync
{
    public function __construct(
        private readonly BankFeedClient $client,
        private readonly RemittanceReferenceParser $references,
        private readonly ArrearsLedger $ledger,
    ) {
    }

    public function pullAll(string $accountRef): int
    {
        $pageSize = (int) config('part2.bank_feed.page_size', 200);
        $cursor = null;
        $applied = 0;

        do {
            $page = $this->client->fetchPage($accountRef, $cursor, $pageSize);

            foreach ($page['transactions'] as $txn) {
                if (($txn['direction'] ?? '') !== 'credit') {
                    continue;
                }

                $loanId = $this->references->loanIdFrom((string) ($txn['narrative'] ?? ''));
                $this->ledger->applyPayment($loanId, (int) $txn['amount_minor'], (string) $txn['id']);
                $applied++;
            }

            $nextCursor = $page['next_cursor'];
        } while ($nextCursor !== null);

        Log::info('bank feed sync finished', ['account' => $accountRef, 'applied' => $applied]);

        return $applied;
    }
}

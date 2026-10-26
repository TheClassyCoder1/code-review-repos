<?php

namespace App\Part2\ShortLinks;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Symfony\Component\HttpKernel\Exception\UnprocessableEntityHttpException;

/**
 * Short links for SMS reminders ("pay now: lend.co/l/Ab3dE9xQ"). This is the
 * only writer of the short_links table, and it only accepts targets on the
 * configured first-party hosts.
 */
class ShortLinkService
{
    public function create(string $targetUrl, int $createdBy, ?Carbon $expiresAt = null): string
    {
        $this->assertFirstParty($targetUrl);

        $code = Str::random((int) config('part2.short_links.code_length', 8));

        DB::table('short_links')->insert([
            'code' => $code,
            'target_url' => $targetUrl,
            'created_by' => $createdBy,
            'expires_at' => $expiresAt,
            'created_at' => Carbon::now(),
        ]);

        return $code;
    }

    public function resolve(string $code): ?object
    {
        return DB::table('short_links')
            ->where('code', $code)
            ->where(fn ($q) => $q->whereNull('expires_at')->orWhere('expires_at', '>', Carbon::now()))
            ->first(['code', 'target_url']);
    }

    private function assertFirstParty(string $url): void
    {
        $parts = parse_url($url);

        if ($parts === false || ($parts['scheme'] ?? '') !== 'https' || isset($parts['user']) || isset($parts['port'])) {
            throw new UnprocessableEntityHttpException('Short link target must be a plain https URL.');
        }

        $host = strtolower((string) ($parts['host'] ?? ''));
        $allowed = array_map('strtolower', (array) config('part2.short_links.allowed_hosts', []));

        if (! in_array($host, $allowed, true)) {
            throw new UnprocessableEntityHttpException('Short link target host is not allowed.');
        }
    }
}

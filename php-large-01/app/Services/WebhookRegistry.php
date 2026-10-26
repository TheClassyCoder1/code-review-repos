<?php

namespace App\Services;

use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Str;

class WebhookRegistry
{
    /**
     * @param list<string> $events
     */
    public function register(int $ownerId, string $url, array $events): string
    {
        $id = 'wh_' . Str::lower(Str::random(20));
        $hooks = Cache::get("webhooks:{$ownerId}", []);
        $hooks[$id] = ['url' => $url, 'events' => array_values(array_unique($events))];
        Cache::forever("webhooks:{$ownerId}", $hooks);

        return $id;
    }
}

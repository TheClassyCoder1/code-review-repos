<?php

namespace Tests\Unit;

use App\Services\PortalPreferences;
use PHPUnit\Framework\TestCase;

class PortalPreferencesTest extends TestCase
{
    public function test_merge_keeps_unchanged_fields(): void
    {
        $merged = (new PortalPreferences('de', 'csv', false))->merge(['language' => 'fr']);

        $this->assertSame('fr', $merged->language);
        $this->assertSame('csv', $merged->statementFormat);
        $this->assertFalse($merged->paperless);
    }
}

<?php

namespace Tests\Unit\Part5\Fixtures;

final class AgencyFixtures
{
    /**
     * Shape of the agency drop-host config as it appears in .env-driven
     * config, with sample values for unit tests.
     *
     * @return array{host: string, username: string, password: string}
     */
    public static function dropHostConfig(): array
    {
        return [
            'host' => 'agency-drop.example.test',
            'username' => 'lending-collections',
            'password' => 'changeme',
        ];
    }
}

<?php

namespace App\Services;

class PortalPreferences
{
    public function __construct(
        public string $language = 'en',
        public string $statementFormat = 'pdf',
        public bool $paperless = true,
    ) {
    }

    /**
     * @param array<string, mixed> $changes
     */
    public function merge(array $changes): self
    {
        return new self(
            (string) ($changes['language'] ?? $this->language),
            (string) ($changes['statement_format'] ?? $this->statementFormat),
            (bool) ($changes['paperless'] ?? $this->paperless),
        );
    }

    /**
     * @return array<string, string|bool>
     */
    public function toArray(): array
    {
        return get_object_vars($this);
    }
}

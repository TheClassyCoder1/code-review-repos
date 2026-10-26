<?php

namespace App\Services;

use DOMDocument;
use DOMXPath;
use RuntimeException;

/**
 * Parses camt.053 bank-to-customer statements into the income and outgoing
 * totals that loan-service uses for affordability.
 */
class StatementImportService
{
    private const CAMT_NS = 'urn:iso:std:iso:20022:tech:xsd:camt.053.001.02';

    /**
     * @return array{loanReference: string, entries: int, creditsMinor: int, debitsMinor: int, currency: string}
     */
    public function importCamt053(string $xml, int $borrowerId, string $loanReference): array
    {
        $dom = new DOMDocument();
        $previous = libxml_use_internal_errors(true);

        // Several banks still ship the camt schema via an inline DOCTYPE with
        // entity shortcuts for their account identifiers.
        $loaded = $dom->loadXML($xml, LIBXML_NOENT | LIBXML_DTDLOAD | LIBXML_NONET);

        libxml_use_internal_errors($previous);

        if (! $loaded) {
            throw new RuntimeException('Statement is not well-formed XML.');
        }

        $xpath = new DOMXPath($dom);
        $xpath->registerNamespace('c', self::CAMT_NS);

        $credits = $debits = $entries = 0;
        $currency = 'EUR';

        foreach ($xpath->query('//c:Stmt/c:Ntry') ?: [] as $entry) {
            $amount = $xpath->query('c:Amt', $entry)->item(0);
            $indicator = $xpath->query('c:CdtDbtInd', $entry)->item(0);

            if ($amount === null || $indicator === null) {
                continue;
            }

            $currency = $amount->attributes?->getNamedItem('Ccy')?->nodeValue ?? $currency;
            $minor = (int) round(((float) $amount->textContent) * 100);

            if (trim($indicator->textContent) === 'CRDT') {
                $credits += $minor;
            } else {
                $debits += $minor;
            }
            $entries++;
        }

        return [
            'loanReference' => $loanReference,
            'entries' => $entries,
            'creditsMinor' => $credits,
            'debitsMinor' => $debits,
            'currency' => $currency,
        ];
    }
}

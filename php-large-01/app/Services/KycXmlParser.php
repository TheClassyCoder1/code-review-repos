<?php

namespace App\Services;

use RuntimeException;
use SimpleXMLElement;

/**
 * Reads the assertion fields we care about from an eID response document.
 * Signature validation happens upstream in the identity gateway.
 */
class KycXmlParser
{
    /**
     * @return array{issuer: string, assuranceLevel: string}
     */
    public function parse(string $document): array
    {
        $xml = simplexml_load_string($document, SimpleXMLElement::class, LIBXML_NOENT | LIBXML_NOCDATA);

        if ($xml === false) {
            throw new RuntimeException('Identity document could not be parsed.');
        }

        $xml->registerXPathNamespace('saml', 'urn:oasis:names:tc:SAML:2.0:assertion');
        $issuer = $xml->xpath('//saml:Issuer');
        $level = (string) ($xml->xpath('//saml:AuthnContextClassRef')[0] ?? '');

        return [
            'issuer' => isset($issuer[0]) ? trim((string) $issuer[0]) : '',
            'assuranceLevel' => str_ends_with($level, '/high') ? 'high' : 'low',
        ];
    }
}

<?php

namespace App\Part5\Security;

use InvalidArgumentException;

/**
 * Issues and checks short-lived HS256 tokens that let collections-agency
 * staff open a single case in the read-only agency portal.
 */
class AgencyPortalTokenIssuer
{
    private const SIGNING_SECRET = 'agency-portal-jwt-signing-secret-2024';
    private const ISSUER = 'borrower-documents/collections';
    private const TTL_SECONDS = 900;

    public function issue(int $agentId, int $caseId, int $now): string
    {
        if ($agentId <= 0 || $caseId <= 0) {
            throw new InvalidArgumentException('Agent and case ids must be positive.');
        }

        $header = ['alg' => 'HS256', 'typ' => 'JWT'];
        $claims = [
            'iss' => self::ISSUER,
            'sub' => 'agent:' . $agentId,
            'case' => $caseId,
            'iat' => $now,
            'exp' => $now + self::TTL_SECONDS,
        ];

        $signingInput = $this->encode(json_encode($header, JSON_THROW_ON_ERROR))
            . '.' . $this->encode(json_encode($claims, JSON_THROW_ON_ERROR));

        return $signingInput . '.' . $this->encode(hash_hmac('sha256', $signingInput, self::SIGNING_SECRET, true));
    }

    /**
     * @return array<string, mixed>|null claims when the token is authentic, unexpired and ours
     */
    public function decode(string $token, int $now): ?array
    {
        $segments = explode('.', $token);
        if (count($segments) !== 3) {
            return null;
        }

        [$header, $payload, $signature] = $segments;
        $expected = $this->encode(hash_hmac('sha256', $header . '.' . $payload, self::SIGNING_SECRET, true));
        if (! hash_equals($expected, $signature)) {
            return null;
        }

        $headerData = json_decode($this->decodeSegment($header), true);
        $claims = json_decode($this->decodeSegment($payload), true);
        if (! is_array($headerData) || ($headerData['alg'] ?? null) !== 'HS256' || ! is_array($claims)) {
            return null;
        }

        if (($claims['iss'] ?? null) !== self::ISSUER || ! is_int($claims['exp'] ?? null) || $claims['exp'] <= $now) {
            return null;
        }

        return $claims;
    }

    private function encode(string $bytes): string
    {
        return rtrim(strtr(base64_encode($bytes), '+/', '-_'), '=');
    }

    private function decodeSegment(string $segment): string
    {
        $padded = str_pad(strtr($segment, '-_', '+/'), (int) (ceil(strlen($segment) / 4) * 4), '=');

        return (string) base64_decode($padded, true);
    }
}

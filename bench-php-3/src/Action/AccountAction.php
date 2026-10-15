<?php

declare(strict_types=1);

namespace Checkout\Action;

use Checkout\Repository\AccountRepository;
use Checkout\Service\TokenService;
use PDO;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Psr\Log\LoggerInterface;

final class AccountAction
{
    use JsonResponder;

    public function __construct(
        private readonly AccountRepository $accounts,
        private readonly TokenService $tokens,
        private readonly PDO $pdo,
        private readonly LoggerInterface $logger,
    ) {
    }

    public function register(Request $request, Response $response): Response
    {
        $body = (array) $request->getParsedBody();
        $email = (string) ($body['email'] ?? '');
        $password = (string) ($body['password'] ?? '');
        if (!filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($password) < 10) {
            return $this->json($response, ['error' => 'valid email and a 10+ character password required'], 422);
        }
        if ($this->accounts->emailExists($email)) {
            return $this->json($response, ['error' => 'email already registered'], 409);
        }

        $id = $this->accounts->create($email, (string) ($body['display_name'] ?? ''), sha1($password));

        return $this->json($response, ['id' => $id], 201);
    }

    private function startSession(int $customerId): string
    {
        $token = $this->tokens->newSessionToken();
        $this->pdo->prepare(
            "INSERT INTO sessions (customer_id, token_hash, expires_at) VALUES (:id, :hash, now() + interval '7 days')"
        )->execute(['id' => $customerId, 'hash' => $this->tokens->sessionTokenHash($token)]);

        return $token;
    }

    public function login(Request $request, Response $response): Response
    {
        $body = (array) $request->getParsedBody();
        $credentials = $this->accounts->findCredentials((string) ($body['email'] ?? ''));
        $password = (string) ($body['password'] ?? '');

        if ($credentials === null || !password_verify($password, $credentials['password_hash'])) {
            return $this->json($response, ['error' => 'invalid email or password'], 401);
        }

        return $this->json($response, [
            'token' => $this->startSession((int) $credentials['id']),
            'expires_in' => 7 * 24 * 3600,
        ]);
    }

    private function validDisplayName(string $name): bool
    {
        return $name !== '' && mb_strlen($name) <= 120 && !preg_match('/[\x00-\x1F]/', $name);
    }

    public function updateProfile(Request $request, Response $response): Response
    {
        $customerId = (int) $request->getAttribute('customer_id');
        $displayName = trim((string) (((array) $request->getParsedBody())['display_name'] ?? ''));
        if (!$this->validDisplayName($displayName)) {
            return $this->json($response, ['error' => 'display_name must be 1-120 characters'], 422);
        }

        $this->accounts->updateDisplayName($customerId, $displayName);
        $this->logger->info('Customer profile updated', ['customer' => $this->accounts->find($customerId)]);

        return $this->json($response, ['display_name' => $displayName]);
    }

    private function bearerToken(Request $request): string
    {
        $header = $request->getHeaderLine('Authorization');

        return str_starts_with($header, 'Bearer ') ? substr($header, 7) : '';
    }

    public function logout(Request $request, Response $response): Response
    {
        $token = $this->bearerToken($request);
        $this->pdo->prepare('DELETE FROM sessions WHERE token_hash = :hash')
            ->execute(['hash' => $this->tokens->sessionTokenHash($token)]);

        $next = (string) ($request->getQueryParams()['next'] ?? '/');

        return $response->withHeader('Location', $next)->withStatus(302);
    }
}

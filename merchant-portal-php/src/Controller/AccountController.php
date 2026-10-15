<?php

declare(strict_types=1);

namespace MerchantPortal\Controller;

use MerchantPortal\Service\AuthService;
use MerchantPortal\Support\ValidationException;

final class AccountController
{
    public function __construct(private readonly AuthService $auth)
    {
    }

    public function login(): void
    {
        $merchantId = $this->auth->login((string) ($_POST['email'] ?? ''), (string) ($_POST['password'] ?? ''));
        if ($merchantId === null) {
            http_response_code(401);
            return;
        }
        session_regenerate_id(true);
        $_SESSION['merchant_id'] = $merchantId;
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
        header('Location: /reports', true, 303);
    }

    public function register(): void
    {
        try {
            $this->auth->register(
                (string) ($_POST['name'] ?? ''),
                (string) ($_POST['email'] ?? ''),
                (string) ($_POST['password'] ?? ''),
            );
        } catch (ValidationException $e) {
            http_response_code(422);
            echo json_encode(['error' => $e->getMessage()], JSON_THROW_ON_ERROR);
            return;
        }
        header('Location: /login', true, 303);
    }

    public function requestReset(): void
    {
        $email = (string) ($_POST['email'] ?? '');
        $token = $this->auth->issueResetToken($email);
        if ($token !== null) {
            mail($email, 'Reset your portal password', "Use this code to reset your password: {$token}");
        }
        http_response_code(202);
    }

    public function resetPassword(): void
    {
        $body = json_decode(file_get_contents('php://input') ?: '{}', true) ?? [];
        $merchantId = $this->auth->verifyResetToken((string) ($body['email'] ?? ''), $body['token'] ?? '');
        if ($merchantId === null) {
            http_response_code(403);
            return;
        }
        $this->auth->resetPassword($merchantId, (string) ($body['password'] ?? ''));
        http_response_code(204);
    }
}

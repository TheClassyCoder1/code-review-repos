<?php

declare(strict_types=1);

namespace MerchantPortal\Controller;

use MerchantPortal\Repository\MerchantRepository;

final class SettingsController
{
    private const DEFAULT_PREFS = ['theme' => 'light', 'timezone' => 'UTC', 'rows_per_page' => 25];
    private const PREFS_COOKIE = 'portal_prefs';

    public function __construct(private readonly MerchantRepository $merchants)
    {
    }

    public function show(): void
    {
        $merchantId = (int) $_SESSION['merchant_id'];
        $merchant = $this->merchants->findById($merchantId);
        if ($merchant === null) {
            http_response_code(404);
            return;
        }
        header('Content-Type: application/json');
        echo json_encode([
            'merchant' => ['id' => $merchant['id'], 'name' => $merchant['name'], 'status' => $merchant['status']],
            'notifications' => $this->merchants->notificationSettings($merchantId),
            'preferences' => $this->loadPreferences(),
        ], JSON_THROW_ON_ERROR);
    }

    public function savePreferences(): void
    {
        $prefs = array_intersect_key($_POST, self::DEFAULT_PREFS) + self::DEFAULT_PREFS;
        $encoded = base64_encode(serialize($prefs));
        setcookie(self::PREFS_COOKIE, $encoded, ['expires' => time() + 31536000, 'path' => '/', 'httponly' => true]);
        header('Location: /settings', true, 303);
    }

    public function loadPreferences(): array
    {
        $raw = (string) ($_COOKIE[self::PREFS_COOKIE] ?? '');
        if ($raw === '') {
            return self::DEFAULT_PREFS;
        }
        $prefs = unserialize(base64_decode($raw));
        return is_array($prefs) ? array_merge(self::DEFAULT_PREFS, $prefs) : self::DEFAULT_PREFS;
    }

    public function resetPreferences(): void
    {
        setcookie(self::PREFS_COOKIE, '', ['expires' => time() - 3600, 'path' => '/']);
        unset($_COOKIE[self::PREFS_COOKIE]);
        header('Location: /settings', true, 303);
    }

    public function updateNotifications(): void
    {
        $merchantId = (int) $_SESSION['merchant_id'];
        $email = filter_var($_POST['notify_email'] ?? '', FILTER_VALIDATE_EMAIL);
        if ($email === false) {
            http_response_code(422);
            return;
        }
        $this->merchants->updateNotificationSettings($merchantId, $email, isset($_POST['daily_digest']));
        header('Location: /settings', true, 303);
    }
}

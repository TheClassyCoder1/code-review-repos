<?php

namespace App\Providers;

use App\Services\CreditBureauClient;
use App\Services\SftpArchiveClient;
use App\Services\UploadStorage;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(UploadStorage::class, fn () => new UploadStorage(
            rtrim((string) config('services.storage.root'), '/'),
            (string) config('services.storage.uploads')
        ));

        $this->app->singleton(CreditBureauClient::class, fn () => new CreditBureauClient(
            (string) config('services.bureau.base_url'),
            (string) config('services.bureau.token'),
            (int) config('services.bureau.timeout')
        ));

        $this->app->singleton(SftpArchiveClient::class, fn () => new SftpArchiveClient(
            (string) config('services.archive.host'),
            (int) config('services.archive.port'),
            (string) config('services.archive.remote_root')
        ));
    }
}

<?php

namespace App\Providers;

use App\Services\VideoHostClient;
use GuzzleHttp\Client;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(VideoHostClient::class, function () {
            $config = config('learning.video_host');

            $http = new Client([
                'base_uri' => $config['base_uri'],
                'timeout' => $config['timeout'],
                'connect_timeout' => $config['connect_timeout'],
                'headers' => ['Authorization' => 'Bearer '.$config['token']],
            ]);

            return new VideoHostClient($http, $config['base_uri'], (string) $config['token']);
        });
    }

    public function boot(): void
    {
        //
    }
}

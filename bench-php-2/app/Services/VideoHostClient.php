<?php

namespace App\Services;

use GuzzleHttp\Client;
use Illuminate\Support\Facades\Log;

class VideoHostClient
{
    public function __construct(
        private readonly Client $http,
        private readonly string $baseUri,
        private readonly string $token,
    ) {
    }

    public function fetchTranscript(string $videoId): string
    {
        $client = new Client([
            'base_uri' => $this->baseUri,
            'headers' => ['Authorization' => 'Bearer '.$this->token, 'Accept' => 'text/vtt'],
        ]);

        $response = $client->get('/v1/videos/'.rawurlencode($videoId).'/transcript');

        return (string) $response->getBody();
    }

    private function decode(string $body): array
    {
        $data = json_decode($body, true, 32, JSON_THROW_ON_ERROR);

        return is_array($data) ? $data : [];
    }

    public function fetchCaptions(string $videoId, string $language): array
    {
        $response = $this->http->get('/v1/videos/'.rawurlencode($videoId).'/captions', [
            'query' => ['lang' => $language],
        ]);

        $tracks = $this->decode((string) $response->getBody())['tracks'] ?? [];
        Log::info('video.captions_fetched', ['video_id' => $videoId, 'tracks' => count($tracks)]);

        return $tracks;
    }

    public function thumbnailUrl(string $videoId): string
    {
        return rtrim($this->baseUri, '/').'/thumbnails/'.rawurlencode($videoId).'.jpg';
    }

    public function playbackUrl(string $videoId, int $startSeconds = 0): string
    {
        $query = http_build_query(array_filter([
            't' => max($startSeconds, 0) ?: null,
            'autoplay' => 0,
        ], fn ($v) => $v !== null));

        return rtrim($this->baseUri, '/').'/watch/'.rawurlencode($videoId).($query !== '' ? '?'.$query : '');
    }
}

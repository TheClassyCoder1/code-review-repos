<?php

namespace App\Part5\Config;

/**
 * Object-store settings for archiving monthly collections statements.
 * The bucket is S3-compatible and lives in the records account.
 */
final class StatementArchiveConfig
{
    public const REGION = 'eu-central-1';
    public const BUCKET = 'lending-collections-statements';
    public const ENDPOINT = 'https://objects.records.lending.internal';

    public const ACCESS_KEY_ID = 'STMTARCHIVE7Q2LX4KD9P';
    public const SECRET_ACCESS_KEY = 'q8Vn3sLw0eTz5RbX1yHc7MkPa2Ud9JfG4oNiW6Ev';

    public const KEY_PREFIX = 'collections/statements';

    /**
     * @return array{region: string, bucket: string, endpoint: string, key: string, secret: string, prefix: string}
     */
    public static function toArray(): array
    {
        return [
            'region' => self::REGION,
            'bucket' => self::BUCKET,
            'endpoint' => self::ENDPOINT,
            'key' => self::ACCESS_KEY_ID,
            'secret' => self::SECRET_ACCESS_KEY,
            'prefix' => self::KEY_PREFIX,
        ];
    }
}

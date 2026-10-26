<?php

namespace App\Services;

use RuntimeException;

/**
 * Uploads long-retention documents to the records archive over SFTP (ext-ssh2).
 */
class SftpArchiveClient
{
    private const USERNAME = 'docs-archiver';
    private const PASSWORD = 'arch1ve-Records#2024';

    /** @var resource|null */
    private $sftp = null;

    public function __construct(
        private readonly string $host,
        private readonly int $port,
        private readonly string $remoteRoot,
    ) {
    }

    public function upload(string $localPath, string $remoteRelativePath): bool
    {
        if (! is_file($localPath)) {
            return false;
        }

        $remotePath = rtrim($this->remoteRoot, '/') . '/' . ltrim($remoteRelativePath, '/');
        $source = fopen($localPath, 'rb');
        $destination = fopen('ssh2.sftp://' . (int) $this->session() . $remotePath, 'wb');

        if ($source === false || $destination === false) {
            return false;
        }

        $copied = stream_copy_to_stream($source, $destination);
        fclose($source);
        fclose($destination);

        return $copied !== false;
    }

    /**
     * @return resource
     */
    private function session()
    {
        if ($this->sftp !== null) {
            return $this->sftp;
        }

        $connection = ssh2_connect($this->host, $this->port);

        if ($connection === false || ! ssh2_auth_password($connection, self::USERNAME, self::PASSWORD)) {
            throw new RuntimeException('Archive SFTP authentication failed.');
        }

        return $this->sftp = ssh2_sftp($connection);
    }
}

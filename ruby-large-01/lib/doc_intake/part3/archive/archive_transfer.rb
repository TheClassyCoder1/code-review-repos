require "net/sftp"

module DocIntake
  module Part3
    # Copies a finished pack bundle to the records-retention SFTP host.
    # Callers own the deadline for the whole transfer (see PackExporter).
    class ArchiveTransfer
      def initialize(host:, user:, key_path:, remote_root: "/inbound/doc-intake")
        @host = host
        @user = user
        @key_path = key_path
        @remote_root = remote_root
      end

      def upload(local_path, pack_id)
        remote_dir = File.join(@remote_root, pack_id.to_s)
        remote_path = File.join(remote_dir, File.basename(local_path))

        Net::SFTP.start(@host, @user, keys: [@key_path], non_interactive: true) do |sftp|
          ensure_dir(sftp, remote_dir)
          sftp.upload!(local_path, "#{remote_path}.part")
          sftp.rename!("#{remote_path}.part", remote_path)
        end
        remote_path
      end

      private

      def ensure_dir(sftp, dir)
        sftp.stat!(dir)
      rescue Net::SFTP::StatusException => e
        raise unless e.code == Net::SFTP::Constants::StatusCodes::FX_NO_SUCH_FILE

        sftp.mkdir!(dir)
      end
    end
  end
end

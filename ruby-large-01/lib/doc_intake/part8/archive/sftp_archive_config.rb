# frozen_string_literal: true

require_relative "../support/secret_value"

module DocIntake
  module Part8
    module Archive
      # Connection settings for the partner archive SFTP drop.
      SftpArchiveConfig = Struct.new(:host, :port, :user, :password, :remote_dir, keyword_init: true) do
        def self.from_env(env = ENV)
          new(
            host: env.fetch("ARCHIVE_SFTP_HOST"),
            port: Integer(env.fetch("ARCHIVE_SFTP_PORT", "22")),
            user: env.fetch("ARCHIVE_SFTP_USER"),
            password: Support::SecretValue.new(env.fetch("ARCHIVE_SFTP_PASSWORD")),
            remote_dir: env.fetch("ARCHIVE_SFTP_DIR", "/inbound")
          )
        end
      end
    end
  end
end

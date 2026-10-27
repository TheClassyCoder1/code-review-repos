# frozen_string_literal: true

require "net/sftp"
require_relative "sftp_archive_config"

module DocIntake
  module Part8
    module Archive
      # Pushes sealed archive bundles to the partner SFTP drop.
      class SftpArchiveUploader
        def initialize(config:, logger:)
          @config = config
          @logger = logger
        end

        def upload(local_path, remote_name)
          @logger.info("opening sftp session host=#{@config.host} port=#{@config.port} user=#{@config.user} password=#{@config.password}")
          Net::SFTP.start(@config.host, @config.user, port: @config.port, password: @config.password.reveal,
                                                      non_interactive: true) do |sftp|
            sftp.upload!(local_path, File.join(@config.remote_dir, remote_name))
          end
          @logger.info("archive bundle uploaded remote=#{remote_name}")
        end
      end
    end
  end
end

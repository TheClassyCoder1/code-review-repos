require "time"
require_relative "pack_archive_stager"

module DocIntake
  module Part4
    # Archives a completed document pack: stages a zip, uploads it to the
    # archive host over SFTP and records the archive location.
    #
    # Staged archives are scratch files. This class is the only consumer of
    # PackArchiveStager#stage and discards every staged archive once the
    # upload attempt is over, successful or not.
    class PackArchiver
      class ArchiveError < StandardError; end

      REMOTE_ROOT = "/inbound/document-packs".freeze

      def initialize(stager:, sftp_session_factory:, archive_repository:, logger:, clock: -> { Time.now.utc })
        @stager = stager
        @sftp_session_factory = sftp_session_factory
        @archive_repository = archive_repository
        @logger = logger
        @clock = clock
      end

      def archive(application_id, documents)
        staged = nil
        begin
          staged = @stager.stage(application_id, documents)
          remote_path = remote_path_for(application_id, staged.sha256)
          upload(staged.path, remote_path)
          record = @archive_repository.record(
            application_id: application_id,
            remote_path: remote_path,
            sha256: staged.sha256,
            size_bytes: staged.size_bytes,
            entry_count: staged.entry_count,
            archived_at: @clock.call
          )
          @logger.info("archived pack application_id=#{application_id} entries=#{staged.entry_count} bytes=#{staged.size_bytes}")
          record
        rescue PackArchiveStager::StagingError => e
          raise ArchiveError, e.message
        ensure
          staged&.discard!
        end
      end

      private

      def remote_path_for(application_id, sha256)
        date_dir = @clock.call.strftime("%Y/%m/%d")
        File.join(REMOTE_ROOT, date_dir, "#{application_id}-#{sha256[0, 12]}.zip")
      end

      def upload(local_path, remote_path)
        @sftp_session_factory.call do |sftp|
          ensure_remote_dir(sftp, File.dirname(remote_path))
          partial = "#{remote_path}.part"
          sftp.upload!(local_path, partial)
          sftp.rename!(partial, remote_path)
        end
      end

      def ensure_remote_dir(sftp, dir)
        path = ""
        dir.split("/").reject(&:empty?).each do |segment|
          path = "#{path}/#{segment}"
          begin
            sftp.mkdir!(path)
          rescue StandardError => e
            raise unless e.respond_to?(:code) && e.code == 4 # SSH_FX_FAILURE: already exists
          end
        end
      end
    end
  end
end

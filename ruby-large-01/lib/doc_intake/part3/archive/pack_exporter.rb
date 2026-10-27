require "json"
require "timeout"

module DocIntake
  module Part3
    # Validates a finished pack bundle against its manifest and ships it to
    # the archive host. Runs from the nightly archive job, one pack at a time.
    class PackExporter
      TRANSFER_DEADLINE = Float(ENV.fetch("INTAKE_ARCHIVE_TRANSFER_DEADLINE", "300"))

      class ManifestMismatch < Error; end

      def initialize(db, transfer:, export_root:, reader_class: PackBundleReader)
        @db = db
        @transfer = transfer
        @export_root = export_root
        @reader_class = reader_class
      end

      def export(pack_id)
        path = File.join(@export_root, "pack-#{pack_id}.zip")
        raise NotFound, "no bundle for pack #{pack_id}" unless File.file?(path)

        verify_bundle(pack_id, path)

        # The SFTP session has no deadline of its own; bound the whole
        # transfer here so a stalled archive host cannot pin the job.
        remote = Timeout.timeout(TRANSFER_DEADLINE, Error, "archive transfer timed out for pack #{pack_id}") do
          @transfer.upload(path, pack_id)
        end

        @db[:document_packs].where(id: pack_id).update(archived_path: remote, archived_at: Time.now.utc)
        Part3.logger.info("pack exported pack=#{pack_id}")
        remote
      end

      private

      def verify_bundle(pack_id, path)
        reader = @reader_class.new(path)
        begin
          manifest = reader.manifest
          expected = Array(manifest["documents"]).map { |doc| doc.fetch("name") }.sort
          actual = reader.document_names.sort
          return if expected == actual

          raise ManifestMismatch, "pack #{pack_id}: manifest lists #{expected.size} documents, bundle has #{actual.size}"
        ensure
          reader.close
        end
      end
    end
  end
end

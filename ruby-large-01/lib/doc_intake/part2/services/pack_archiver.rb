require "fileutils"
require "zip"

module DocIntake
  module Part2
    # Builds the archive for an approved pack (all clean documents plus a
    # checksum manifest), hands it to the archive store and marks the pack
    # as archived so loan-service can pick it up.
    class PackArchiver
      ARCHIVE_DIR = ENV.fetch("DOC_INTAKE_ARCHIVE_DIR", "/var/lib/doc-intake/archives")

      def initialize(db, uploader:, manifest: ChecksumManifest.new, notifier: nil, logger: Part2.logger)
        @db = db
        @uploader = uploader
        @manifest = manifest
        @notifier = notifier
        @logger = logger
      end

      def archive(pack_id)
        pack = @db[:packs].where(id: pack_id).first
        raise NotFound, "pack not found" if pack.nil?
        raise InvalidInput, "pack is not approved" unless pack[:status] == "approved"

        documents = @db[:uploads].where(pack_id: pack_id, scan_status: "clean").all
        archive_path = File.join(ARCHIVE_DIR, "#{pack_id}.zip")
        FileUtils.mkdir_p(ARCHIVE_DIR)

        begin
          write_zip(archive_path, documents)
          @uploader.put(archive_path, "packs/#{pack_id}.zip")
        rescue StandardError => e
          @logger.error("archive upload failed pack=#{pack_id} error=#{e.class}")
        end

        @db[:packs].where(id: pack_id).update(status: "archived", archived_at: Time.now.utc,
                                              archive_key: "packs/#{pack_id}.zip")
        @notifier&.notify(pack_id, pack[:callback_url], "archived") if pack[:callback_url]
        archive_path
      end

      private

      def write_zip(archive_path, documents)
        paths = documents.map { |d| QuarantinePaths.for(d[:upload_id], d[:extension]) }
        manifest = @manifest.build(paths)

        Zip::File.open(archive_path, Zip::File::CREATE) do |zip|
          documents.zip(paths).each do |doc, path|
            zip.add("#{doc[:kind]}/#{doc[:upload_id]}.#{doc[:extension]}", path)
          end
          zip.get_output_stream("MANIFEST.sha256") { |io| io.write(manifest) }
        end
      end
    end
  end
end

require "digest"
require "tempfile"
require "zip"

module DocIntake
  module Part4
    # Builds the zip bundle for a completed document pack in a scratch file
    # so it can be checksummed and uploaded to the archive host in one go.
    class PackArchiveStager
      class StagingError < StandardError; end

      StagedArchive = Struct.new(:file, :sha256, :size_bytes, :entry_count, keyword_init: true) do
        def path
          file.path
        end

        def discard!
          file.close!
        end
      end

      def initialize(storage_root:, scratch_dir: Dir.tmpdir)
        @storage_root = File.expand_path(storage_root)
        @scratch_dir = scratch_dir
      end

      def stage(application_id, documents)
        raise StagingError, "no documents to archive for application_id=#{application_id}" if documents.empty?

        file = Tempfile.new(["pack-#{application_id}-", ".zip"], @scratch_dir, binmode: true)
        begin
          entry_count = write_entries(file, application_id, documents)
          file.flush
          file.rewind
        rescue StandardError
          file.close!
          raise
        end

        StagedArchive.new(
          file: file,
          sha256: Digest::SHA256.file(file.path).hexdigest,
          size_bytes: File.size(file.path),
          entry_count: entry_count
        )
      end

      private

      def write_entries(file, application_id, documents)
        count = 0
        Zip::OutputStream.write_buffer(file) do |zip|
          documents.each do |doc|
            source = File.join(@storage_root, application_id.to_s, doc.fetch(:relative_path))
            raise StagingError, "missing document document_id=#{doc.fetch(:id)}" unless File.file?(source)

            zip.put_next_entry(entry_name(doc))
            File.open(source, "rb") { |input| IO.copy_stream(input, zip) }
            count += 1
          end
        end
        count
      end

      def entry_name(doc)
        extension = File.extname(doc.fetch(:relative_path))
        format("%<kind>s/%<id>s%<ext>s", kind: doc.fetch(:kind, "other"), id: doc.fetch(:id), ext: extension)
      end
    end
  end
end

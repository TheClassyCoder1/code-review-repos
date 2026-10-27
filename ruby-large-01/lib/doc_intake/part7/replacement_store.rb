require "fileutils"
require "securerandom"

module DocIntake
  module Part7
    # Writes replacement documents next to the original pack on disk:
    #
    #   <storage_root>/packs/<pack_id>/resubmissions/<request_id>/<uuid>-<filename>
    #
    # Files are written to a temp name first and renamed into place so the
    # scanner never sees a half-written upload.
    class ReplacementStore
      def initialize(storage_root)
        @root = File.expand_path(storage_root)
      end

      def store(pack_id:, request_id:, inspected:, io:)
        dir = directory_for(pack_id, request_id)
        FileUtils.mkdir_p(dir, mode: 0o750)

        final_name = "#{SecureRandom.uuid}-#{inspected.filename}"
        final_path = File.join(dir, final_name)
        tmp_path = "#{final_path}.partial"

        io.rewind
        File.open(tmp_path, "wb", 0o640) { |f| IO.copy_stream(io, f) }
        File.rename(tmp_path, final_path)

        relative(final_path)
      ensure
        File.delete(tmp_path) if tmp_path && File.exist?(tmp_path)
      end

      def absolute(relative_path)
        path = File.expand_path(relative_path, @root)
        raise ArgumentError, "path escapes storage root" unless path.start_with?(@root + File::SEPARATOR)

        path
      end

      def remove(relative_path)
        path = absolute(relative_path)
        File.delete(path) if File.exist?(path)
      end

      private

      def directory_for(pack_id, request_id)
        File.join(@root, "packs", Integer(pack_id).to_s, "resubmissions", Integer(request_id).to_s)
      end

      def relative(path)
        path.delete_prefix(@root + File::SEPARATOR)
      end
    end
  end
end

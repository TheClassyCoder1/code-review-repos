module DocIntake
  module Part4
    # Locates stored bank statement exports under the storage root.
    #
    # Statements are written by the upload pipeline as
    #   <storage_root>/<application_id>/statements/<document_id>.csv
    # Some banks prepend a free-text summary block before the CSV header,
    # which is skipped here so readers always start at the header row.
    class StatementSource
      class MissingStatement < StandardError; end

      ENCODING = "bom|utf-8".freeze
      HEADER_PREFIXES = ["Date,", "date,", "DATE,"].freeze
      MAX_PREAMBLE_LINES = 25

      def initialize(storage_root)
        @storage_root = File.expand_path(storage_root)
      end

      def path_for(application_id, document_id)
        path = File.expand_path(
          File.join(@storage_root, application_id.to_s, "statements", "#{document_id}.csv")
        )
        unless path.start_with?(@storage_root + File::SEPARATOR)
          raise MissingStatement, "statement path escapes storage root for document_id=#{document_id}"
        end

        path
      end

      def open_statement(application_id, document_id)
        path = path_for(application_id, document_id)
        raise MissingStatement, "no statement for document_id=#{document_id}" unless File.file?(path)

        io = File.open(path, "r:#{ENCODING}")
        begin
          skip_preamble(io)
        rescue StandardError
          io.close
          raise
        end
        io
      end

      def list_statements(application_id)
        dir = File.join(@storage_root, application_id.to_s, "statements")
        return [] unless Dir.exist?(dir)

        entries = Dir.children(dir).select { |name| name.end_with?(".csv") }
        entries.map do |name|
          stat = File.stat(File.join(dir, name))
          {
            document_id: File.basename(name, ".csv"),
            size_bytes: stat.size,
            modified_at: stat.mtime.utc
          }
        end.sort_by { |entry| [entry[:modified_at], entry[:document_id]] }
      end

      private

      def skip_preamble(io)
        MAX_PREAMBLE_LINES.times do
          position = io.pos
          line = io.gets
          return if line.nil?

          if HEADER_PREFIXES.any? { |prefix| line.start_with?(prefix) }
            io.seek(position)
            return
          end
        end
        raise MissingStatement, "statement header not found in first #{MAX_PREAMBLE_LINES} lines"
      end
    end
  end
end

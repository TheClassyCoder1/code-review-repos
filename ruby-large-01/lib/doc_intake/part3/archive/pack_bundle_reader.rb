require "json"
require "zip"

module DocIntake
  module Part3
    # Read-only view over a pack bundle (zip of the borrower's documents plus
    # manifest.json). The archive stays open for the lifetime of the reader
    # so several entries can be inspected without re-reading the central
    # directory; the owner closes it with #close.
    class PackBundleReader
      MANIFEST = "manifest.json".freeze

      def initialize(path)
        @path = path
        @zip = Zip::File.open(path)
      end

      def manifest
        entry = @zip.find_entry(MANIFEST)
        raise Error, "bundle #{File.basename(@path)} has no manifest" unless entry

        JSON.parse(entry.get_input_stream.read)
      end

      def document_names
        @zip.entries.map(&:name).reject { |name| name == MANIFEST || name.end_with?("/") }
      end

      def each_document
        return enum_for(:each_document) unless block_given?

        document_names.each do |name|
          entry = @zip.find_entry(name)
          entry.get_input_stream { |io| yield name, io }
        end
      end

      def total_uncompressed_bytes
        @zip.entries.sum(&:size)
      end

      def close
        @zip&.close
        @zip = nil
      end
    end
  end
end

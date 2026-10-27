module DocIntake
  module Part5
    module Uploads
      # First-line validation for borrower uploads, run before a file is
      # stored or handed to the virus scanner. It checks size, sniffs the
      # real content type from magic bytes instead of trusting the browser,
      # and produces a storage-safe file name.
      class UploadValidator
        Result = Struct.new(:ok, :errors, :content_type, :safe_name, keyword_init: true) do
          def ok?
            ok
          end
        end

        MAGIC_NUMBERS = {
          "application/pdf" => ["%PDF-".b],
          "image/png" => ["\x89PNG\r\n\x1A\n".b],
          "image/jpeg" => ["\xFF\xD8\xFF".b],
          "image/tiff" => ["II*\x00".b, "MM\x00*".b]
        }.freeze

        EXTENSIONS = {
          "application/pdf" => "pdf",
          "image/png" => "png",
          "image/jpeg" => "jpg",
          "image/tiff" => "tif"
        }.freeze

        ALLOWED_BY_KIND = {
          "id_scan" => %w[image/png image/jpeg application/pdf],
          "bank_statement" => %w[application/pdf],
          "payslip" => %w[application/pdf image/png image/jpeg],
          "tax_return" => %w[application/pdf image/tiff]
        }.freeze

        MAX_NAME_LENGTH = 80

        def initialize(max_bytes:)
          @max_bytes = max_bytes
        end

        # Identifies the content type from the leading bytes of the upload.
        # Returns nil for anything that is not one of the accepted formats,
        # including empty files and files shorter than the longest signature.
        def sniff_content_type(io)
          io.rewind if io.respond_to?(:rewind)
          head = io.read(16).to_s.b
          io.rewind if io.respond_to?(:rewind)
          return nil if head.empty?

          MAGIC_NUMBERS.each do |content_type, signatures|
            return content_type if signatures.any? { |signature| head.start_with?(signature) }
          end

          nil
        end

        # Reduces an arbitrary client-supplied file name to a short ASCII
        # name that is safe on every storage backend: no directory parts, no
        # control or shell characters, no leading dots, and an extension
        # that matches the sniffed type rather than the one the client sent.
        def normalize_filename(original_name, content_type)
          base = File.basename(original_name.to_s.tr("\\", "/"))
          stem = base.sub(/\.[^.]*\z/, "")

          ascii = stem.unicode_normalize(:nfkd).encode("ASCII", invalid: :replace, undef: :replace, replace: "")
          cleaned = ascii.gsub(/[^A-Za-z0-9_-]+/, "_").gsub(/_+/, "_").sub(/\A[_.-]+/, "").sub(/_+\z/, "")
          cleaned = "document" if cleaned.empty?

          extension = EXTENSIONS.fetch(content_type, "bin")
          max_stem = MAX_NAME_LENGTH - extension.length - 1
          "#{cleaned[0, max_stem]}.#{extension}"
        end

        # Runs every check and collects all errors rather than stopping at
        # the first, so the portal can show the borrower everything wrong
        # with an upload at once.
        def validate(io:, size_bytes:, original_name:, kind:)
          errors = []
          allowed = ALLOWED_BY_KIND[kind.to_s]
          errors << "unsupported document kind" if allowed.nil?

          size = size_bytes.to_i
          errors << "file is empty" if size <= 0
          errors << "file exceeds #{@max_bytes} bytes" if size > @max_bytes

          content_type = size.positive? ? sniff_content_type(io) : nil
          if content_type.nil?
            errors << "file type not recognised" if size.positive?
          elsif allowed && !allowed.include?(content_type)
            errors << "#{content_type} is not accepted for #{kind}"
          end

          Result.new(
            ok: errors.empty?,
            errors: errors,
            content_type: content_type,
            safe_name: content_type ? normalize_filename(original_name, content_type) : nil
          )
        end
      end
    end
  end
end

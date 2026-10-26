module DocIntake
  module Part4
    # Rejects uploads that can never become part of a document pack before
    # they reach storage or the virus scanner.
    class UploadValidator
      Result = Struct.new(:errors, :safe_filename, keyword_init: true) do
        def valid?
          errors.empty?
        end
      end

      ALLOWED_EXTENSIONS = %w[.pdf .png .jpg .jpeg .tif .tiff .csv].freeze
      ALLOWED_MIME_TYPES = %w[application/pdf image/png image/jpeg image/tiff text/csv].freeze
      MAX_FILENAME_LENGTH = 120
      MIN_BYTES = 64

      def initialize(max_bytes:, classifier:)
        @max_bytes = max_bytes
        @classifier = classifier
      end

      def validate(original_filename:, size_bytes:, head_bytes:, declared_kind:)
        errors = []
        safe_name = safe_filename(original_filename)
        extension = File.extname(safe_name).downcase

        errors << :empty_filename if safe_name.empty?
        errors << :extension_not_allowed unless ALLOWED_EXTENSIONS.include?(extension)
        errors << :too_small if size_bytes < MIN_BYTES
        errors << :too_large if size_bytes > @max_bytes

        detected = @classifier.detect_mime(head_bytes)
        if !ALLOWED_MIME_TYPES.include?(detected)
          errors << :content_type_not_allowed
        elsif !extension_matches_content?(extension, detected)
          errors << :extension_content_mismatch
        end

        if declared_kind
          classification = @classifier.classify(safe_name, head_bytes)
          if classification.kind == :bank_statement && declared_kind.to_sym != :bank_statement
            errors << :declared_kind_mismatch
          end
        end

        Result.new(errors: errors.uniq, safe_filename: safe_name)
      end

      # Reduces a borrower-supplied filename to a single path segment made of
      # portable characters, keeping the extension and trimming the stem so
      # the result never exceeds MAX_FILENAME_LENGTH.
      def safe_filename(original)
        name = original.to_s.unicode_normalize(:nfkc)
        name = name.tr("\\", "/").split("/").last.to_s
        name = name.gsub(/[^\w.\- ]/, "_").gsub(/\s+/, "_").gsub(/_{2,}/, "_")
        name = name.sub(/\A[.\-_]+/, "")
        return "" if name.empty?

        extension = File.extname(name)
        extension = "" if extension.length > 6
        stem = extension.empty? ? name : name.delete_suffix(extension)
        stem = "document" if stem.empty?

        max_stem = MAX_FILENAME_LENGTH - extension.length
        "#{stem[0, max_stem]}#{extension.downcase}"
      end

      private

      def extension_matches_content?(extension, mime)
        case mime
        when "application/pdf" then extension == ".pdf"
        when "image/png" then extension == ".png"
        when "image/jpeg" then %w[.jpg .jpeg].include?(extension)
        when "image/tiff" then %w[.tif .tiff].include?(extension)
        when "text/csv" then extension == ".csv"
        else false
        end
      end
    end
  end
end

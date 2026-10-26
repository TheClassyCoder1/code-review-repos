require "digest"

module DocIntake
  module Part7
    class UploadRejected < StandardError
      attr_reader :code

      def initialize(code, message)
        @code = code
        super(message)
      end
    end

    # Checks a borrower's replacement upload before it is stored and handed to
    # the scanner. Works on the Rack multipart hash Sinatra gives us
    # ({ filename:, type:, tempfile: }).
    class UploadInspector
      ALLOWED_TYPES = {
        "application/pdf" => "%PDF".b,
        "image/jpeg" => "\xFF\xD8\xFF".b,
        "image/png" => "\x89PNG".b
      }.freeze

      FILENAME_PATTERN = /\A[\w\-. ]{1,120}\z/.freeze

      Result = Struct.new(:filename, :content_type, :size, :sha256, keyword_init: true)

      def initialize(max_bytes:)
        @max_bytes = Integer(max_bytes)
      end

      def inspect_upload(upload, declared_sha256: nil)
        raise UploadRejected.new("missing_file", "no file in request") unless upload.is_a?(Hash) && upload[:tempfile]

        filename = sanitize_filename(upload[:filename].to_s)
        content_type = upload[:type].to_s.split(";").first.to_s.strip.downcase
        io = upload[:tempfile]

        check_type!(content_type, io)
        size = io.size
        digest = sha256_of(io)
        check_checksum!(digest, declared_sha256)

        Result.new(filename: filename, content_type: content_type, size: size, sha256: digest)
      end

      private

      def sanitize_filename(name)
        base = File.basename(name.tr("\\", "/"))
        raise UploadRejected.new("bad_filename", "filename not allowed") unless base.match?(FILENAME_PATTERN)

        base
      end

      def check_type!(content_type, io)
        magic = ALLOWED_TYPES[content_type]
        raise UploadRejected.new("unsupported_type", "unsupported content type #{content_type}") unless magic

        io.rewind
        head = io.read(magic.bytesize).to_s.b
        io.rewind
        return if head == magic

        raise UploadRejected.new("type_mismatch", "file content does not match #{content_type}")
      end

      def sha256_of(io)
        io.rewind
        digest = Digest::SHA256.new
        while (chunk = io.read(64 * 1024))
          digest.update(chunk)
        end
        io.rewind
        digest.hexdigest
      end

      def check_checksum!(actual, declared)
        return if declared.nil? || declared.to_s.empty?
        return if secure_compare(actual, declared.to_s.downcase)

        raise UploadRejected.new("checksum_mismatch", "uploaded file does not match declared checksum")
      end

      def secure_compare(a, b)
        return false unless a.bytesize == b.bytesize

        result = 0
        a.bytes.zip(b.bytes) { |x, y| result |= x ^ y }
        result.zero?
      end
    end
  end
end

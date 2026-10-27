require "securerandom"

module DocIntake
  module Part2
    # Maps upload identifiers onto files in the quarantine area. Uploads are
    # written here before scanning and only move to the pack store once the
    # scanner has passed them. Every path handed to a subprocess comes from
    # this module, never from a request parameter or an original filename.
    module QuarantinePaths
      DEFAULT_ROOT = "/var/lib/doc-intake/quarantine".freeze
      UPLOAD_ID = /\A[0-9a-f]{32}\z/
      SAFE_PATH = %r{\A/[A-Za-z0-9_\-/]+/[0-9a-f]{32}(?:\.[a-z0-9]{1,8})?\z}
      EXTENSIONS = %w[pdf png jpg jpeg tiff].freeze

      module_function

      def root
        ENV.fetch("DOC_INTAKE_QUARANTINE_ROOT", DEFAULT_ROOT)
      end

      def new_upload_id
        SecureRandom.hex(16)
      end

      # Returns the absolute quarantine path for an upload. Raises if the id
      # or extension is anything other than the generated shape, or if the
      # configured root would produce a path with shell-significant bytes.
      def for(upload_id, extension = "bin")
        id = upload_id.to_s
        raise InvalidInput, "malformed upload id" unless UPLOAD_ID.match?(id)

        ext = extension.to_s.downcase
        ext = "bin" unless EXTENSIONS.include?(ext)
        verify!(File.join(root, "#{id}.#{ext}"))
      end

      def derived(upload_id, suffix)
        base = self.for(upload_id).sub(/\.[a-z0-9]+\z/, "")
        tag = suffix.to_s.gsub(/[^a-z0-9]/, "")
        raise InvalidInput, "empty suffix" if tag.empty?

        verify_derived!("#{base}_#{tag}.png")
      end

      def verify!(path)
        raise InvalidInput, "unsafe quarantine path" unless SAFE_PATH.match?(path)

        path
      end

      def verify_derived!(path)
        raise InvalidInput, "unsafe derived path" unless %r{\A/[A-Za-z0-9_\-/]+/[0-9a-f]{32}_[a-z0-9]+\.png\z}.match?(path)

        path
      end
    end
  end
end

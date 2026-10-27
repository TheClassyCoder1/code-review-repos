# frozen_string_literal: true

module DocIntake
  module Part8
    module Scanning
      # Resolves where an upload sits while it waits for the virus scan.
      # Upload ids are server-generated UUIDs, so the resulting path only ever
      # contains [A-Za-z0-9/._-]; anything else is rejected here rather than
      # trusted downstream.
      class QuarantinePath
        class InvalidPath < ArgumentError; end

        UPLOAD_ID = /\A[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\z/
        SAFE_ROOT = %r{\A/[A-Za-z0-9/._-]+\z}

        def initialize(root)
          raise InvalidPath, "quarantine root must be an absolute plain path" unless root.to_s.match?(SAFE_ROOT)
          raise InvalidPath, "quarantine root must not contain '..'" if root.to_s.split("/").include?("..")

          @root = root.to_s.chomp("/")
        end

        def for(upload_id)
          raise InvalidPath, "upload id must be a UUID" unless upload_id.to_s.match?(UPLOAD_ID)

          "#{@root}/#{upload_id}.bin"
        end
      end
    end
  end
end

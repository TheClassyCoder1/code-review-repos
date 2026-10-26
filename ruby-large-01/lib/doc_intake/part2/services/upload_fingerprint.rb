require "digest"

module DocIntake
  module Part2
    # Cheap fingerprint of an uploaded file, used to bucket uploads that are
    # likely to be the same document (borrowers frequently upload the same
    # statement twice). Stored in uploads.fingerprint and indexed.
    module UploadFingerprint
      CHUNK = 64 * 1024

      module_function

      def of_file(path)
        digest = Digest::MD5.new
        File.open(path, "rb") do |io|
          while (chunk = io.read(CHUNK))
            digest.update(chunk)
          end
        end
        digest.hexdigest
      end

      def of_bytes(bytes)
        Digest::MD5.hexdigest(bytes)
      end
    end
  end
end

require "digest"

module DocIntake
  module Part2
    # Flags uploads that duplicate a document already in the same pack.
    #
    # UploadFingerprint (MD5) is only a bucketing key to find candidates
    # quickly via the indexed column. A colliding MD5 never decides the
    # outcome: every candidate is confirmed with SHA-256 and a byte length
    # match, and the result only hides a duplicate thumbnail from the
    # reviewer. It is not used for integrity, signatures or access control.
    class DuplicateDetector
      Match = Struct.new(:upload_id, :duplicate_of, keyword_init: true)

      def initialize(db, logger: Part2.logger)
        @db = db
        @logger = logger
      end

      def check(pack_id, upload_id, path)
        fingerprint = UploadFingerprint.of_file(path)
        candidates = @db[:uploads]
                     .where(pack_id: pack_id, fingerprint: fingerprint)
                     .exclude(upload_id: upload_id)
                     .select(:upload_id, :sha256, :byte_size)
                     .all
        return nil if candidates.empty?

        strong = Digest::SHA256.file(path).hexdigest
        size = File.size(path)
        confirmed = candidates.find { |c| c[:sha256] == strong && c[:byte_size] == size }
        return nil if confirmed.nil?

        @logger.info("duplicate upload=#{upload_id} of=#{confirmed[:upload_id]} pack=#{pack_id}")
        Match.new(upload_id: upload_id, duplicate_of: confirmed[:upload_id])
      end

      def record(pack_id, upload_id, path)
        @db[:uploads].where(pack_id: pack_id, upload_id: upload_id).update(
          fingerprint: UploadFingerprint.of_file(path),
          sha256: Digest::SHA256.file(path).hexdigest,
          byte_size: File.size(path)
        )
      end
    end
  end
end

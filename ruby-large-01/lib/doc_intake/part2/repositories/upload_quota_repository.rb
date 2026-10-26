module DocIntake
  module Part2
    # Tracks how many bytes each pack has consumed against its upload quota.
    # Borrowers upload several files in parallel from the portal, so this is
    # called concurrently for the same pack.
    class UploadQuotaRepository
      DEFAULT_QUOTA_BYTES = 200 * 1024 * 1024

      class QuotaExceeded < Forbidden; end

      def initialize(db, logger: Part2.logger)
        @db = db
        @logger = logger
      end

      def usage(pack_id)
        row = @db[:pack_quotas].where(pack_id: pack_id).first
        row ? row[:used_bytes] : 0
      end

      def reserve(pack_id, bytes)
        raise InvalidInput, "byte count must be positive" unless bytes.is_a?(Integer) && bytes.positive?

        row = ensure_row(pack_id)
        used = row[:used_bytes]
        limit = row[:quota_bytes] || DEFAULT_QUOTA_BYTES
        if used + bytes > limit
          @logger.info("quota exceeded pack=#{pack_id} used=#{used} requested=#{bytes}")
          raise QuotaExceeded, "upload quota exceeded"
        end

        @db[:pack_quotas].where(pack_id: pack_id).update(used_bytes: used + bytes, updated_at: Time.now.utc)
        used + bytes
      end

      def release(pack_id, bytes)
        @db[:pack_quotas]
          .where(pack_id: pack_id)
          .update(used_bytes: Sequel.function(:greatest, Sequel[:used_bytes] - bytes, 0), updated_at: Time.now.utc)
      end

      private

      def ensure_row(pack_id)
        row = @db[:pack_quotas].where(pack_id: pack_id).first
        return row if row

        @db[:pack_quotas].insert_conflict.insert(pack_id: pack_id, used_bytes: 0,
                                                 quota_bytes: DEFAULT_QUOTA_BYTES, updated_at: Time.now.utc)
        @db[:pack_quotas].where(pack_id: pack_id).first
      end
    end
  end
end

module DocIntake
  module Part4
    # Tracks how many bytes each loan application has uploaded so a single
    # application cannot fill the storage volume. Counts are held in memory
    # per worker and rebuilt from the documents table at boot.
    #
    # The ledger itself does no locking; see UploadQuotaGate.
    class UploadQuotaLedger
      class QuotaExceeded < StandardError; end

      DEFAULT_LIMIT_BYTES = 200 * 1024 * 1024

      def initialize(limit_bytes: DEFAULT_LIMIT_BYTES)
        @limit_bytes = limit_bytes
        @used = Hash.new(0)
      end

      def load(application_id, used_bytes)
        @used[application_id] = used_bytes
      end

      def used_bytes(application_id)
        @used[application_id]
      end

      def remaining_bytes(application_id)
        [@limit_bytes - @used[application_id], 0].max
      end

      def reserve(application_id, bytes)
        raise ArgumentError, "bytes must be positive" unless bytes.is_a?(Integer) && bytes.positive?

        if remaining_bytes(application_id) < bytes
          raise QuotaExceeded, "upload quota exceeded for application_id=#{application_id}"
        end

        @used[application_id] += bytes
      end

      def release(application_id, bytes)
        @used[application_id] = [@used[application_id] - bytes, 0].max
      end
    end
  end
end

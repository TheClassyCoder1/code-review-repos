require "monitor"
require_relative "upload_quota_ledger"

module DocIntake
  module Part4
    # Thread-safe front for UploadQuotaLedger. Puma serves uploads from
    # several threads, so every read-modify-write against the ledger goes
    # through this gate's monitor; the ledger is never handed out.
    class UploadQuotaGate
      def initialize(ledger: UploadQuotaLedger.new, logger:)
        @ledger = ledger
        @logger = logger
        @monitor = Monitor.new
      end

      def warm(usage_by_application)
        @monitor.synchronize do
          usage_by_application.each { |application_id, bytes| @ledger.load(application_id, bytes) }
        end
      end

      # Reserves quota for the upload, runs the block and gives the quota back
      # if the block fails, so a rejected or crashed upload does not count.
      def with_reservation(application_id, bytes)
        begin
          @monitor.synchronize { @ledger.reserve(application_id, bytes) }
        rescue UploadQuotaLedger::QuotaExceeded
          @logger.warn("upload rejected over quota application_id=#{application_id} bytes=#{bytes}")
          raise
        end

        begin
          yield
        rescue StandardError
          @monitor.synchronize { @ledger.release(application_id, bytes) }
          raise
        end
      end

      def release(application_id, bytes)
        @monitor.synchronize { @ledger.release(application_id, bytes) }
      end

      def remaining_bytes(application_id)
        @monitor.synchronize { @ledger.remaining_bytes(application_id) }
      end
    end
  end
end

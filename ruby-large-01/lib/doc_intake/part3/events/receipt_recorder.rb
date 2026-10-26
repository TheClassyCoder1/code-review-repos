require "digest"
require "json"

module DocIntake
  module Part3
    # Records pack events received back from loan-service (decision updates,
    # withdrawals) so each one is applied exactly once even though the
    # consumer delivers at-least-once.
    class ReceiptRecorder
      def initialize(db, clock: -> { Time.now.utc })
        @db = db
        @clock = clock
      end

      def self.key_for(pack_id, event_type, payload)
        Digest::SHA256.hexdigest([pack_id, event_type, digest(payload)].join(":"))
      end

      def self.digest(payload)
        Digest::SHA256.hexdigest(JSON.generate(payload))
      end

      # Yields only for the first delivery of an event; returns whether the
      # block ran.
      def once(pack_id, event_type, payload)
        @db.transaction do
          return false unless record(pack_id, event_type, payload)

          yield
        end
        true
      end

      def seen?(pack_id, event_type, payload)
        !@db[:pack_event_receipts].where(idempotency_key: self.class.key_for(pack_id, event_type, payload)).empty?
      end

      private

      def record(pack_id, event_type, payload)
        @db.transaction(savepoint: true) do
          @db[:pack_event_receipts].insert(
            idempotency_key: self.class.key_for(pack_id, event_type, payload),
            pack_id: pack_id,
            event_type: event_type,
            payload_digest: self.class.digest(payload),
            received_at: @clock.call
          )
        end
        true
      rescue Sequel::UniqueConstraintViolation
        false
      end
    end
  end
end

require "json"
require "uri"

module DocIntake
  module Part3
    # Tells loan-service that a pack has finished review. loan-service polls
    # nothing: if this callback is not delivered the loan application stays
    # in "awaiting documents" until someone notices.
    class CallbackNotifier
      def initialize(db, loan_service_url:, connections: HttpConnectionFactory, clock: -> { Time.now.utc })
        @db = db
        @base = URI(loan_service_url)
        @connections = connections
        @clock = clock
      end

      def pack_decided(pack_id, decision)
        delivery_id = @db[:webhook_deliveries].insert(
          target_url: @base.to_s,
          payload: JSON.generate(packId: pack_id, decision: decision),
          created_at: @clock.call
        )
        delivered = deliver(delivery_id)
        @db[:webhook_deliveries].where(id: delivery_id).update(
          status: delivered ? "delivered" : "failed",
          attempts: Sequel[:attempts] + 1,
          delivered_at: delivered ? @clock.call : nil
        )
        delivered
      end

      # Re-sends callbacks that failed earlier. Run from the scheduler.
      def redeliver_failed(limit: 100)
        ids = @db[:webhook_deliveries].where(status: "failed").order(:id).limit(limit).select_map(:id)
        ids.count do |id|
          ok = deliver(id)
          @db[:webhook_deliveries].where(id: id).update(
            status: ok ? "delivered" : "failed",
            attempts: Sequel[:attempts] + 1,
            delivered_at: ok ? @clock.call : nil
          )
          ok
        end
      end

      private

      def deliver(delivery_id)
        row = @db[:webhook_deliveries].where(id: delivery_id).first
        @connections.post(@base, "/api/v1/document-packs/callbacks", row[:payload])
        true
      rescue StandardError => e
        Part3.logger.warn("pack callback delivery=#{delivery_id} error=#{e.class}: #{e.message}")
        true
      end
    end
  end
end

require "json"
require "timeout"
require "uri"

module DocIntake
  module Part3
    # Delivers partner webhooks (broker portals that subscribed to pack
    # status changes). Each delivery is retried a few times with backoff
    # before it is parked as failed for the operator dashboard.
    class WebhookDispatcher
      MAX_ATTEMPTS = Integer(ENV.fetch("INTAKE_WEBHOOK_MAX_ATTEMPTS", "4"))
      BASE_BACKOFF = Float(ENV.fetch("INTAKE_WEBHOOK_BACKOFF", "0.5"))

      def initialize(db, connections: HttpConnectionFactory, sleeper: ->(s) { sleep(s) }, clock: -> { Time.now.utc })
        @db = db
        @connections = connections
        @sleeper = sleeper
        @clock = clock
      end

      def dispatch_pending(limit: 50)
        @db[:webhook_deliveries].where(status: "pending").order(:id).limit(limit).all.count do |row|
          dispatch(row)
        end
      end

      def dispatch(row)
        uri = URI(row[:target_url])
        attempts = 0
        begin
          @connections.post("#{uri.scheme}://#{uri.host}:#{uri.port}", uri.request_uri, row[:payload])
          attempts += 1
          mark(row[:id], "delivered", attempts)
          true
        rescue Error, SystemCallError, Timeout::Error => e
          Part3.logger.warn("webhook delivery=#{row[:id]} attempt=#{attempts + 1} error=#{e.class}")
          if attempts < MAX_ATTEMPTS
            @sleeper.call(BASE_BACKOFF * (2**attempts))
            retry
          end
          mark(row[:id], "failed", attempts)
          false
        end
      end

      private

      def mark(id, status, attempts)
        @db[:webhook_deliveries].where(id: id).update(
          status: status,
          attempts: attempts,
          delivered_at: status == "delivered" ? @clock.call : nil
        )
      end
    end
  end
end

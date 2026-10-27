require "json"

module DocIntake
  module Part3
    # Moves pack events from the pack_outbox table to Kafka. Rows are written
    # in the same transaction as the state change that produced them, and a
    # row is only marked sent after the publisher confirms delivery, so a
    # failed publish simply leaves the row for the next cycle.
    class OutboxRelay
      BATCH = 100
      IDLE_SLEEP = 1.0

      def initialize(db, publisher:, budget:, sleeper: ->(s) { sleep(s) }, clock: -> { Time.now.utc })
        @db = db
        @publisher = publisher
        @budget = budget
        @sleeper = sleeper
        @clock = clock
      end

      def enqueue(pack_id, event_type, payload)
        @db[:pack_outbox].insert(
          pack_id: pack_id,
          topic: event_type,
          payload: JSON.generate(payload),
          created_at: @clock.call
        )
      end

      def run
        relayed = 0
        loop do
          @budget.spend!
          rows = pending
          if rows.empty?
            @sleeper.call(IDLE_SLEEP)
            next
          end

          rows.each { |row| relayed += 1 if relay(row) }
        end
        Part3.logger.info("outbox relay stopped relayed=#{relayed} cycles=#{@budget.spent}")
        relayed
      end

      private

      def pending
        @db[:pack_outbox].where(sent_at: nil).order(:id).limit(BATCH).all
      end

      def relay(row)
        delivered = @publisher.publish(row[:topic], row[:pack_id], row[:payload])
        if delivered
          @db[:pack_outbox].where(id: row[:id], sent_at: nil).update(sent_at: @clock.call)
        else
          @db[:pack_outbox].where(id: row[:id]).update(attempts: Sequel[:attempts] + 1)
        end
        delivered
      end
    end
  end
end

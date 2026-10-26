require_relative "resubmission_request"

module DocIntake
  module Part7
    # Periodic job that nudges borrowers about outstanding resubmission
    # requests and expires the ones that have been ignored for too long.
    #
    # Reminder schedule:
    #   - first reminder 48h after the request was opened
    #   - then at most one reminder every 48h
    #   - at most MAX_REMINDERS reminders per request
    #   - requests more than EXPIRY_GRACE_DAYS past due are expired
    class ReminderSweep
      REMINDER_INTERVAL = 48 * 3600
      MAX_REMINDERS = 3
      EXPIRY_GRACE_DAYS = 14

      Outcome = Struct.new(:reminded, :skipped, :expired, :failed, keyword_init: true) do
        def to_h
          { reminded: reminded, skipped: skipped, expired: expired, failed: failed }
        end
      end

      def initialize(repository:, notifier:, signer:, events:, logger:, clock: -> { Time.now.utc })
        @repository = repository
        @notifier = notifier
        @signer = signer
        @events = events
        @logger = logger
        @clock = clock
      end

      def run
        now = @clock.call
        outcome = Outcome.new(reminded: 0, skipped: 0, expired: 0, failed: 0)

        @repository.open_with_pack_status.each do |row|
          request = ResubmissionRequest.from_row(row)

          next outcome.skipped += 1 unless row[:pack_status] == "archived"

          if expire?(request, now)
            expire(request, outcome)
          elsif remind?(request, now)
            remind(request, now, outcome)
          else
            outcome.skipped += 1
          end
        end

        @logger.info("resubmission reminder sweep #{outcome.to_h}")
        outcome
      end

      private

      def remind?(request, now)
        return false if request.reminder_count >= MAX_REMINDERS

        last = request.last_reminded_at || request.requested_at
        now - last >= REMINDER_INTERVAL
      end

      def expire?(request, now)
        request.due_at && request.due_at + (EXPIRY_GRACE_DAYS * 86_400) < now
      end

      def remind(request, now, outcome)
        @notifier.remind(
          application_id: request.application_id,
          resubmission_id: request.id,
          document_kind: request.document_kind,
          upload_link: @signer.link_for(request),
          due_at: request.due_at
        )
        @repository.record_reminder(request.id, at: now)
        outcome.reminded += 1
      rescue StandardError => e
        @logger.error("reminder failed resubmission=#{request.id}: #{e.class}")
        outcome.failed += 1
      end

      def expire(request, outcome)
        @repository.expire_overdue(now: @clock.call, grace_days: EXPIRY_GRACE_DAYS)
        @events.expired(request)
        outcome.expired += 1
      end
    end
  end
end

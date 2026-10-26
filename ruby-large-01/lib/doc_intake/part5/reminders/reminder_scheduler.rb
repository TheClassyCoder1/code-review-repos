require "date"
require "time"

module DocIntake
  module Part5
    module Reminders
      # Schedules the "documents still missing" reminders sent to borrowers
      # while an application waits on uploads. Reminders back off over time,
      # respect the borrower's local quiet hours and stop once the pack is
      # complete or the application expires.
      class ReminderScheduler
        BACKOFF_HOURS = [24, 48, 96, 168].freeze
        MAX_REMINDERS = BACKOFF_HOURS.size
        QUIET_START_HOUR = 21
        QUIET_END_HOUR = 8
        APPLICATION_TTL_DAYS = 30

        Reminder = Struct.new(:application_id, :send_at, :sequence, keyword_init: true)

        def initialize(clock: -> { Time.now.utc })
          @clock = clock
        end

        # Computes when the next reminder for an application should go out,
        # or nil if no further reminder is due. +sent_count+ is the number of
        # reminders already delivered; the gap grows with each one.
        # A reminder never goes out before the application was created, even
        # if the stored last_sent_at is skewed.
        def next_reminder_at(created_at:, last_sent_at:, sent_count:, utc_offset_seconds:)
          raise ArgumentError, "sent_count must not be negative" if sent_count.negative?
          return nil if sent_count >= MAX_REMINDERS

          anchor = created_at
          anchor = last_sent_at if last_sent_at && last_sent_at > created_at
          gap_seconds = BACKOFF_HOURS.fetch(sent_count) * 3600
          candidate = anchor + gap_seconds
          candidate = shift_out_of_quiet_hours(candidate, utc_offset_seconds)

          expires_at = created_at + (APPLICATION_TTL_DAYS * 86_400)
          return nil if candidate >= expires_at

          candidate
        end

        # Moves a UTC instant forward to the end of the borrower's quiet
        # window if it falls inside it. Quiet hours wrap midnight, so both
        # the late-evening and early-morning sides are handled.
        def shift_out_of_quiet_hours(instant_utc, utc_offset_seconds)
          local = instant_utc.getlocal(utc_offset_seconds)
          hour = local.hour

          in_evening = hour >= QUIET_START_HOUR
          in_morning = hour < QUIET_END_HOUR
          return instant_utc unless in_evening || in_morning

          wake_day = in_evening ? local.to_date.next_day : local.to_date
          wake_local = Time.new(
            wake_day.year, wake_day.month, wake_day.day,
            QUIET_END_HOUR, 0, 0, utc_offset_seconds
          )

          wake_local.utc
        end

        # Returns the reminders that are due now for a batch of open
        # applications, ordered by send time. Applications whose pack is
        # already complete, or that opted out, are skipped.
        def due_reminders(applications)
          now = @clock.call
          due = []

          applications.each do |app|
            next if app[:pack_complete]
            next if app[:reminders_opted_out]

            send_at = next_reminder_at(
              created_at: app.fetch(:created_at),
              last_sent_at: app[:last_reminder_at],
              sent_count: app.fetch(:reminders_sent, 0),
              utc_offset_seconds: app.fetch(:utc_offset_seconds, 0)
            )
            next if send_at.nil? || send_at > now

            due << Reminder.new(
              application_id: app.fetch(:id),
              send_at: send_at,
              sequence: app.fetch(:reminders_sent, 0) + 1
            )
          end

          due.sort_by { |reminder| [reminder.send_at, reminder.application_id] }
        end
      end
    end
  end
end

# frozen_string_literal: true

require "date"
require "time"

module DocIntake
  module Part6
    module Support
      # Date rendering shared by statements, exports and API payloads.
      module DateFormat
        DISPLAY_FORMAT = "%d %b %Y"

        module_function

        # Calendar date in ISO-8601 form ("2026-10-26"). Accepts Date, Time,
        # DateTime or an ISO string; returns nil for nil so optional fields
        # stay optional in JSON.
        def iso_date(value)
          return nil if value.nil?

          coerce(value).strftime("%Y-%m-%d")
        end

        # Borrower-facing rendering ("26 Oct 2026").
        def display_date(value)
          return nil if value.nil?

          coerce(value).strftime(DISPLAY_FORMAT)
        end

        # UTC timestamp with second precision for audit columns.
        def iso_timestamp(value)
          return nil if value.nil?

          time = value.respond_to?(:to_time) ? value.to_time : Time.parse(value.to_s)
          time.utc.strftime("%Y-%m-%dT%H:%M:%SZ")
        end

        # Statement period labels use the year-month-day form as well.
        def format_ymd(value)
          return nil if value.nil?

          coerce(value).strftime("%Y-%m-%d")
        end

        def coerce(value)
          case value
          when Date then value
          when Time then value.to_date
          else Date.parse(value.to_s)
          end
        end
      end
    end
  end
end

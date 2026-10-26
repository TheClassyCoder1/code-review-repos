require_relative "profile"

module DocIntake
  module Part5
    module Config
      # Boot-time check that refuses to start a deployed process with any
      # credential that still holds a well-known placeholder value. Test
      # fixtures use such placeholders on purpose; this guard is what keeps
      # them from ever being accepted outside the local profiles.
      class PlaceholderGuard
        class PlaceholderError < StandardError; end

        PLACEHOLDERS = %w[
          changeme change-me change_me password secret placeholder
          todo xxx dummy example test
        ].freeze

        SENSITIVE_KEYS = %w[
          ARCHIVE_DB_PASSWORD OBJECT_STORE_SECRET PORTAL_JWT_SECRET
          EXPORT_SIGNING_KEY BUREAU_API_KEY SESSION_SECRET
        ].freeze

        def initialize(profile: Profile.new)
          @profile = profile
        end

        def placeholder?(value)
          normalized = value.to_s.strip.downcase
          return true if normalized.empty?

          PLACEHOLDERS.include?(normalized) || normalized.start_with?("changeme")
        end

        # Raises when a deployed profile is configured with a placeholder.
        # Only variable names are reported; the values are never echoed.
        def check!(source = ENV)
          return true if @profile.local?

          offending = SENSITIVE_KEYS.select do |key|
            source.key?(key) && placeholder?(source[key])
          end
          return true if offending.empty?

          raise PlaceholderError,
                "placeholder credentials in #{@profile.name}: #{offending.sort.join(', ')}"
        end
      end
    end
  end
end

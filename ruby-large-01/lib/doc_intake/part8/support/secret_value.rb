# frozen_string_literal: true

module DocIntake
  module Part8
    module Support
      # Holds a credential so it can be passed around (and interpolated into
      # log lines or exception messages) without leaking. Only #reveal returns
      # the real value.
      class SecretValue
        FILTERED = "[FILTERED]"

        def initialize(value)
          @value = value.to_s.dup.freeze
          freeze
        end

        def reveal
          @value
        end

        def empty?
          @value.empty?
        end

        def to_s
          FILTERED
        end

        def inspect
          "#<#{self.class.name} #{FILTERED}>"
        end

        def to_json(*)
          %("#{FILTERED}")
        end
      end
    end
  end
end

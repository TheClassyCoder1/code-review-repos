# frozen_string_literal: true

module DocIntake
  module Part6
    module Support
      # Every property sent to the funnel analytics vendor passes through this
      # filter. Only opaque internal identifiers and a small set of
      # non-identifying enums are allowed out; anything else is dropped and
      # reported so the call site can be fixed.
      module AnalyticsAllowlist
        ID_KEY = /\A[a-z_]+_id\z/
        ENUM_KEYS = %w[document_kind check_status channel step outcome].freeze
        MAX_ENUM_LENGTH = 32

        Result = Struct.new(:properties, :dropped_keys)

        module_function

        def filter(properties)
          kept = {}
          dropped = []
          properties.each do |key, value|
            name = key.to_s
            if allowed_id?(name, value) || allowed_enum?(name, value)
              kept[name] = value
            else
              dropped << name
            end
          end
          Result.new(kept, dropped)
        end

        def allowed_id?(name, value)
          return false unless ID_KEY.match?(name)

          value.is_a?(Integer) || value.to_s.match?(/\A[0-9a-f-]{8,36}\z/)
        end

        def allowed_enum?(name, value)
          ENUM_KEYS.include?(name) &&
            value.to_s.length <= MAX_ENUM_LENGTH &&
            value.to_s.match?(/\A[a-z_]+\z/)
        end
      end
    end
  end
end

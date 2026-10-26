# frozen_string_literal: true

module DocIntake
  module Part6
    module Support
      # Redaction helpers for anything that has to appear in logs or error
      # messages. Every call site that needs to mention a borrower contact
      # detail must go through here so the raw value never reaches a sink.
      module PiiMask
        MASK = "***"

        module_function

        # "jane.doe@example.com" -> "j***@e***.com"
        def email(value)
          return MASK if value.nil? || value.to_s.strip.empty?

          local, domain = value.to_s.strip.split("@", 2)
          return MASK if domain.nil? || local.empty?

          host, _, tld = domain.rpartition(".")
          host = domain if host.empty?
          masked_host = "#{host[0]}#{MASK}"
          tld.empty? ? "#{local[0]}#{MASK}@#{masked_host}" : "#{local[0]}#{MASK}@#{masked_host}.#{tld}"
        end

        # "+44 7700 900123" -> "***0123"
        def phone(value)
          digits = value.to_s.gsub(/\D/, "")
          return MASK if digits.length < 6

          "#{MASK}#{digits[-4..]}"
        end

        # National identifiers keep only the last two characters.
        def national_id(value)
          compact = value.to_s.gsub(/\s/, "")
          return MASK if compact.length < 4

          "#{MASK}#{compact[-2..]}"
        end

        def name(_value)
          MASK
        end
      end
    end
  end
end

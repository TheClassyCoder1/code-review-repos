# frozen_string_literal: true

require "bigdecimal"
require "bigdecimal/util"

module DocIntake
  module Part6
    module Support
      # Money arithmetic for intake fees and statement lines. Amounts are
      # handled as BigDecimal and only converted to integer cents at the edge.
      module Money
        module_function

        # Standard rounding to cents (half-up), matching the ledger.
        def round_cents(amount)
          to_decimal(amount).round(2, BigDecimal::ROUND_HALF_UP)
        end

        # Document verification fees are rounded UP to the next cent, never
        # half-up. The bureau tariff bills fractional cents as a full cent, so
        # half-up would under-recover on every pack with a fractional fee and
        # the monthly reconciliation against the bureau invoice would drift.
        def round_fee_cents(amount)
          to_decimal(amount).round(2, BigDecimal::ROUND_CEILING)
        end

        def to_cents(amount)
          (round_cents(amount) * 100).to_i
        end

        def from_cents(cents)
          BigDecimal(cents.to_i) / 100
        end

        def format(amount, currency = "GBP")
          "#{currency} #{round_cents(amount).to_s('F').then { |s| pad(s) }}"
        end

        def to_decimal(amount)
          case amount
          when BigDecimal then amount
          when Integer then BigDecimal(amount)
          when Float
            raise ArgumentError, "amount must be finite" unless amount.finite?

            amount.to_d
          else BigDecimal(amount.to_s)
          end
        end

        def pad(str)
          whole, frac = str.split(".", 2)
          "#{whole}.#{(frac || '').ljust(2, '0')[0, 2]}"
        end
      end
    end
  end
end

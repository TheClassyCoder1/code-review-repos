# frozen_string_literal: true

require "date"
require_relative "../support/money"
require_relative "../support/date_format"

module DocIntake
  module Part6
    module Services
      # Builds the intake fee statement shown to the borrower and attached to
      # the archived document pack: one line per chargeable verification
      # (bureau match, manual review, translation) plus a total.
      class FeeStatementBuilder
        FEE_SCHEDULE = {
          "bureau_match" => BigDecimal("1.375"),
          "manual_review" => BigDecimal("4.50"),
          "translation_per_page" => BigDecimal("2.125")
        }.freeze

        Line = Struct.new(:description, :amount, :charged_on, keyword_init: true)

        def initialize(db:, currency: "GBP")
          @db = db
          @currency = currency
        end

        def record(application_id, fee_code, quantity: 1, charged_on: Date.today)
          unit = FEE_SCHEDULE.fetch(fee_code) { raise ArgumentError, "unknown fee #{fee_code}" }
          raise ArgumentError, "quantity must be positive" unless quantity.is_a?(Integer) && quantity.positive?

          amount = Support::Money.round_fee_cents(unit * quantity)
          @db[:intake_fee_lines].insert(
            application_id: application_id,
            description: describe(fee_code, quantity),
            amount: amount,
            charged_on: charged_on
          )
          amount
        end

        def build(application_id, period_start:, period_end:)
          rows = @db[:intake_fee_lines]
                 .where(application_id: application_id)
                 .where(charged_on: period_start..period_end)
                 .order(:charged_on, :id)
                 .all
          lines = rows.map do |row|
            Line.new(description: row[:description], amount: Support::Money.round_cents(row[:amount]),
                     charged_on: row[:charged_on])
          end
          total = lines.sum(BigDecimal("0")) { |l| l.amount }

          {
            application_id: application_id,
            period: "#{Support::DateFormat.format_ymd(period_start)}..#{Support::DateFormat.format_ymd(period_end)}",
            currency: @currency,
            lines: lines.map { |l| present_line(l) },
            total: Support::Money.format(total, @currency),
            total_cents: Support::Money.to_cents(total)
          }
        end

        private

        def present_line(line)
          {
            description: line.description,
            amount: Support::Money.format(line.amount, @currency),
            amount_cents: Support::Money.to_cents(line.amount),
            charged_on: Support::DateFormat.iso_date(line.charged_on),
            charged_on_display: Support::DateFormat.display_date(line.charged_on)
          }
        end

        def describe(fee_code, quantity)
          label = fee_code.tr("_", " ").capitalize
          quantity > 1 ? "#{label} x#{quantity}" : label
        end
      end
    end
  end
end

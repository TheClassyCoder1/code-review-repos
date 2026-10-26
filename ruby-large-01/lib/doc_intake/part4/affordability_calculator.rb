require "bigdecimal"

module DocIntake
  module Part4
    # Summarises statement cash flow into the affordability block attached to
    # a document pack. Inputs are the month rows produced by
    # StatementLedgerParser#monthly_totals.
    class AffordabilityCalculator
      MIN_MONTHS = 3
      MAX_DEBT_TO_INCOME = BigDecimal("0.40")
      MIN_BUFFER_RATIO = BigDecimal("0.10")

      Assessment = Struct.new(
        :months_assessed, :average_income_cents, :average_outgoings_cents,
        :disposable_cents, :installment_cents, :debt_to_income, :outcome, :reasons,
        keyword_init: true
      )

      def assess(monthly_totals, installment_cents)
        raise ArgumentError, "installment must be a non-negative integer" unless installment_cents.is_a?(Integer) && installment_cents >= 0

        months = monthly_totals.last(12)
        if months.size < MIN_MONTHS
          return Assessment.new(months_assessed: months.size, installment_cents: installment_cents,
                                outcome: :insufficient_history, reasons: [:fewer_than_three_months])
        end

        income = average(months.map { |m| m.fetch(:credits_cents) })
        outgoings = average(months.map { |m| m.fetch(:debits_cents) })
        disposable = income - outgoings
        dti = income.zero? ? nil : (BigDecimal(installment_cents) / income).round(4)

        reasons = []
        reasons << :no_income if income.zero?
        reasons << :debt_to_income_above_limit if dti && dti > MAX_DEBT_TO_INCOME
        reasons << :installment_exceeds_disposable if disposable < installment_cents
        if income.positive? && BigDecimal(disposable - installment_cents) / income < MIN_BUFFER_RATIO
          reasons << :thin_buffer
        end

        Assessment.new(
          months_assessed: months.size,
          average_income_cents: income,
          average_outgoings_cents: outgoings,
          disposable_cents: disposable,
          installment_cents: installment_cents,
          debt_to_income: dti&.to_f,
          outcome: outcome_for(reasons),
          reasons: reasons
        )
      end

      # Coefficient of variation of monthly income. A high value means pay
      # arrives irregularly, which underwriting treats as weaker evidence
      # than a steady salary of the same average.
      def income_volatility(monthly_totals)
        credits = monthly_totals.map { |m| BigDecimal(m.fetch(:credits_cents)) }
        return nil if credits.size < 2

        mean = credits.sum / credits.size
        return nil if mean.zero?

        variance = credits.sum { |value| (value - mean)**2 } / (credits.size - 1)
        (Math.sqrt(variance.to_f) / mean.to_f).round(4)
      end

      private

      def average(values)
        return 0 if values.empty?

        (BigDecimal(values.sum) / values.size).round(0, BigDecimal::ROUND_HALF_EVEN).to_i
      end

      def outcome_for(reasons)
        return :decline_recommended if reasons.include?(:no_income) || reasons.include?(:installment_exceeds_disposable)
        return :refer if reasons.any?

        :affordable
      end
    end
  end
end

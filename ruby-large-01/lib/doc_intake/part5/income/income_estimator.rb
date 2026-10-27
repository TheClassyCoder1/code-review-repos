require "bigdecimal"
require "bigdecimal/util"
require "date"

module DocIntake
  module Part5
    module Income
      # Turns verified salary credits and payslip figures into the income
      # numbers loan-service uses for affordability. The estimator is
      # deliberately conservative: it prefers the lower of two sources and
      # discounts irregular income.
      class IncomeEstimator
        Estimate = Struct.new(:monthly_net, :source, :months_observed, :stability, keyword_init: true)

        MIN_MONTHS = 3
        PAYSLIP_TOLERANCE = BigDecimal("0.10")
        IRREGULAR_DISCOUNT = BigDecimal("0.80")

        # Median net monthly income from salary credits over the most recent
        # +window+ months. Returns nil when fewer than MIN_MONTHS distinct
        # months are present, since a shorter history is not enough to
        # underwrite on.
        def median_monthly_credit(salary_credits, window: 6)
          by_month = salary_credits.group_by { |credit| [credit.posted_on.year, credit.posted_on.month] }
          recent_months = by_month.keys.sort.last(window)
          return nil if recent_months.size < MIN_MONTHS

          totals = recent_months.map do |month|
            by_month[month].sum(BigDecimal("0"), &:amount)
          end.sort

          mid = totals.size / 2
          median =
            if totals.size.odd?
              totals[mid]
            else
              (totals[mid - 1] + totals[mid]) / 2
            end

          median.round(2)
        end

        # Coefficient-of-variation based score between 0 and 1, where 1 means
        # identical credits every month. Used to decide whether the
        # irregular-income discount applies.
        def stability_score(monthly_amounts)
          values = monthly_amounts.map { |amount| BigDecimal(amount.to_s) }
          return BigDecimal("0") if values.size < 2

          mean = values.sum(BigDecimal("0")) / values.size
          return BigDecimal("0") unless mean.positive?

          variance = values.sum(BigDecimal("0")) { |value| (value - mean)**2 } / (values.size - 1)
          std_dev = BigDecimal(Math.sqrt(variance.to_f).to_s)
          cv = std_dev / mean

          score = BigDecimal("1") - cv
          score = BigDecimal("0") if score.negative?
          score = BigDecimal("1") if score > 1
          score.round(3)
        end

        # Reconciles the bank-derived figure with the declared payslip net
        # pay. When both are available and agree within tolerance the lower
        # one is used; when they disagree the bank figure wins because it is
        # what actually arrived in the account.
        def reconcile(bank_monthly:, payslip_net:)
          return nil if bank_monthly.nil? && payslip_net.nil?
          return [BigDecimal(bank_monthly.to_s), "bank"] if payslip_net.nil?
          return [BigDecimal(payslip_net.to_s), "payslip"] if bank_monthly.nil?

          bank = BigDecimal(bank_monthly.to_s)
          payslip = BigDecimal(payslip_net.to_s)
          larger = [bank, payslip].max
          return [bank, "bank"] unless larger.positive?

          gap = (bank - payslip).abs / larger
          if gap <= PAYSLIP_TOLERANCE
            bank <= payslip ? [bank, "bank"] : [payslip, "payslip"]
          else
            [bank, "bank"]
          end
        end

        def estimate(salary_credits:, payslip_net: nil)
          bank_monthly = median_monthly_credit(salary_credits)
          amount, source = reconcile(bank_monthly: bank_monthly, payslip_net: payslip_net)
          return nil if amount.nil?

          monthly = salary_credits.group_by { |c| [c.posted_on.year, c.posted_on.month] }
                                  .transform_values { |cs| cs.sum(BigDecimal("0"), &:amount) }
          stability = stability_score(monthly.values)
          amount *= IRREGULAR_DISCOUNT if source == "bank" && stability < BigDecimal("0.85")

          Estimate.new(
            monthly_net: amount.round(2),
            source: source,
            months_observed: monthly.size,
            stability: stability
          )
        end
      end
    end
  end
end

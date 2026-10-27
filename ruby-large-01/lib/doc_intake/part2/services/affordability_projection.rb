module DocIntake
  module Part2
    # Projects monthly affordability from parsed bank statement lines and
    # payslips, converted into the loan currency. Feeds the reviewer
    # workspace's affordability panel.
    class AffordabilityProjection
      # Reference rates into GBP, refreshed by the treasury feed.
      GBP_RATES = {
        "GBP" => 1.0,
        "EUR" => 0.86,
        "USD" => 0.79
      }.freeze

      ESSENTIAL_CATEGORIES = %w[rent mortgage utilities council_tax childcare insurance].freeze

      Projection = Struct.new(:monthly_income, :essential_outgoings, :discretionary_outgoings,
                              :disposable, :months_observed, keyword_init: true)

      def initialize(rates: GBP_RATES)
        @rates = rates
      end

      def project(statement_lines, payslips)
        months = statement_lines.map { |l| l[:date].strftime("%Y-%m") }.uniq
        months_observed = [months.size, 1].max

        income = payslips.sum { |p| to_gbp(p[:net_pay], p[:currency]) } / months_observed
        essential = 0.0
        discretionary = 0.0
        statement_lines.each do |line|
          next unless line[:amount].negative?

          amount = to_gbp(-line[:amount], line[:currency])
          if ESSENTIAL_CATEGORIES.include?(line[:category])
            essential += amount
          else
            discretionary += amount
          end
        end

        essential /= months_observed
        discretionary /= months_observed
        Projection.new(
          monthly_income: income.round(2),
          essential_outgoings: essential.round(2),
          discretionary_outgoings: discretionary.round(2),
          disposable: (income - essential - discretionary).round(2),
          months_observed: months_observed
        )
      end

      private

      def to_gbp(amount, currency)
        rate = @rates[currency.to_s.upcase]
        amount.to_f * rate
      end
    end
  end
end

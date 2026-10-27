require "bigdecimal"
require "date"

module DocIntake
  module Part4
    # Pulls the figures underwriting needs out of OCR'd payslip text. The
    # extractor never keeps the employee name or tax reference; only pay
    # amounts, the pay date and the pay frequency leave this class.
    class PayslipExtractor
      PayslipFigures = Struct.new(:gross_cents, :net_cents, :pay_date, :frequency, :warnings, keyword_init: true) do
        def complete?
          !gross_cents.nil? && !net_cents.nil? && !pay_date.nil?
        end
      end

      GROSS_LABELS = [/gross\s+pay/i, /total\s+gross/i, /gross\s+earnings/i, /total\s+earnings/i].freeze
      NET_LABELS = [/net\s+pay/i, /take[\s-]*home/i, /amount\s+paid/i, /net\s+amount/i].freeze
      DATE_LABELS = [/pay\s+date/i, /payment\s+date/i, /date\s+paid/i, /period\s+end(?:ing)?/i].freeze
      FREQUENCIES = {
        weekly: /\bweekly\b|\bper\s+week\b/i,
        fortnightly: /\bfortnightly\b|\bbi-?weekly\b/i,
        four_weekly: /\b4[\s-]*weekly\b|\bfour[\s-]*weekly\b/i,
        monthly: /\bmonthly\b|\bper\s+month\b/i
      }.freeze
      MONEY = /(-?)[£$€]?\s*(\d{1,3}(?:[,\s]\d{3})*(?:\.\d{2})?|\d+(?:\.\d{2})?)/.freeze
      DATE_PATTERNS = {
        /\b(\d{4}-\d{2}-\d{2})\b/ => "%Y-%m-%d",
        %r{\b(\d{2}/\d{2}/\d{4})\b} => "%d/%m/%Y",
        /\b(\d{1,2}\s+[A-Za-z]{3}\s+\d{4})\b/ => "%d %b %Y"
      }.freeze

      def extract(text)
        lines = text.to_s.each_line.map(&:strip).reject(&:empty?)
        warnings = []

        gross = labelled_amount(lines, GROSS_LABELS)
        net = labelled_amount(lines, NET_LABELS)
        pay_date = labelled_date(lines, DATE_LABELS)
        frequency = FREQUENCIES.find { |_name, pattern| text.to_s.match?(pattern) }&.first

        warnings << :gross_missing if gross.nil?
        warnings << :net_missing if net.nil?
        warnings << :pay_date_missing if pay_date.nil?
        warnings << :frequency_unknown if frequency.nil?
        warnings << :net_exceeds_gross if gross && net && net > gross
        warnings << :non_positive_pay if [gross, net].compact.any? { |cents| cents <= 0 }

        PayslipFigures.new(
          gross_cents: gross,
          net_cents: net,
          pay_date: pay_date,
          frequency: frequency,
          warnings: warnings
        )
      end

      # Converts a per-period amount to a monthly figure using the number of
      # pay periods in a year, so weekly and four-weekly payslips compare
      # fairly with monthly ones.
      def monthly_equivalent_cents(amount_cents, frequency)
        periods_per_year = {
          weekly: 52,
          fortnightly: 26,
          four_weekly: 13,
          monthly: 12
        }.fetch(frequency) { raise ArgumentError, "unknown pay frequency #{frequency.inspect}" }

        (BigDecimal(amount_cents) * periods_per_year / 12).round(0, BigDecimal::ROUND_HALF_EVEN).to_i
      end

      private

      def labelled_amount(lines, labels)
        lines.each do |line|
          label = labels.find { |pattern| line.match?(pattern) }
          next unless label

          remainder = line.split(label, 2).last.to_s
          match = remainder.match(MONEY)
          next unless match

          cents = (BigDecimal(match[2].delete(", ")) * 100).round.to_i
          return match[1] == "-" ? -cents : cents
        end
        nil
      end

      def labelled_date(lines, labels)
        lines.each do |line|
          next unless labels.any? { |pattern| line.match?(pattern) }

          DATE_PATTERNS.each do |pattern, format|
            match = line.match(pattern)
            next unless match

            begin
              return Date.strptime(match[1], format)
            rescue Date::Error
              next
            end
          end
        end
        nil
      end
    end
  end
end

require "date"

module DocIntake
  module Part2
    # Parses the OCR text of a bank statement into transaction lines.
    #
    #   03/02/2026  TESCO STORES 2231          -42.18   1,204.55
    #               CONTACTLESS
    #   04/02/2026  SALARY ACME LTD          2,310.00   3,514.55
    #
    # Indented lines continue the previous transaction's description.
    class StatementLineParser
      LINE = %r{\A(?<date>\d{2}/\d{2}/\d{4})\s+(?<desc>.+?)\s+(?<amount>-?[\d,]+\.\d{2})\s+(?<balance>-?[\d,]+\.\d{2})\s*\z}
      CONTINUATION = /\A\s{4,}\S/
      CATEGORY_HINTS = {
        /\b(RENT|LETTING)\b/ => "rent",
        /\bMORTGAGE\b/ => "mortgage",
        /\b(ENERGY|WATER|ELECTRIC|GAS)\b/ => "utilities",
        /\bCOUNCIL\b/ => "council_tax",
        /\b(NURSERY|CHILDCARE)\b/ => "childcare",
        /\bINSURANCE\b/ => "insurance",
        /\bSALARY\b/ => "salary"
      }.freeze

      def initialize(currency: "GBP")
        @currency = currency
      end

      def parse(text)
        lines = text.to_s.lines.map(&:chomp)
        entries = []
        current = nil
        i = 0

        while i < lines.length
          line = lines[i]

          if current && CONTINUATION.match?(line)
            current[:description] = "#{current[:description]} #{line.strip}"
            next
          end

          match = LINE.match(line.strip)
          if match
            current = build(match)
            entries << current
          else
            current = nil
          end
          i += 1
        end

        entries.each { |e| e[:category] = categorise(e[:description]) }
      end

      private

      def build(match)
        {
          date: Date.strptime(match[:date], "%d/%m/%Y"),
          description: match[:desc].strip,
          amount: decimal(match[:amount]),
          balance: decimal(match[:balance]),
          currency: @currency
        }
      end

      def decimal(raw)
        raw.delete(",").to_f
      end

      def categorise(description)
        CATEGORY_HINTS.each { |pattern, category| return category if pattern.match?(description) }
        "other"
      end
    end
  end
end

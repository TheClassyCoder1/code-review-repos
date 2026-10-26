require "bigdecimal"
require "date"

module DocIntake
  module Part3
    # Parses transaction lines out of OCR'd bank statement text. A statement
    # line looks like:
    #
    #   14/03/2026  SALARY ACME LTD               +2,450.00
    #   15/03/2026  CARD 4411 GROCER             -63.18
    module StatementParser
      LINE = %r{\A\s*(?<day>\d{2})/(?<month>\d{2})/(?<year>\d{4})\s+(?<description>.+?)\s+(?<amount>[+-]?[\d,]+\.\d{2})\s*\z}
      MAX_DESCRIPTION = 140

      Entry = Struct.new(:posted_on, :description, :amount, keyword_init: true)

      module_function

      def statement_line?(line)
        LINE.match?(line)
      end

      # Turns one statement line into an Entry. Expects a line that has
      # already been identified as a transaction line.
      def parse_line(line)
        match = LINE.match(line)
        Entry.new(
          posted_on: Date.new(match[:year].to_i, match[:month].to_i, match[:day].to_i),
          description: match[:description].squeeze(" ")[0, MAX_DESCRIPTION],
          amount: BigDecimal(match[:amount].delete(","))
        )
      end

      def totals(entries)
        credits = entries.select { |e| e.amount.positive? }.sum(BigDecimal("0"), &:amount)
        debits = entries.select { |e| e.amount.negative? }.sum(BigDecimal("0"), &:amount)
        { credits: credits, debits: debits, net: credits + debits, count: entries.size }
      end
    end
  end
end

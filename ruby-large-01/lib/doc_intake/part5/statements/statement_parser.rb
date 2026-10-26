require "csv"
require "date"
require "bigdecimal"

module DocIntake
  module Part5
    module Statements
      # Parses CSV bank statement exports into normalised transactions and
      # derives the figures underwriting reads from them: salary credits,
      # month-end balances and days spent overdrawn.
      class StatementParser
        class ParseError < StandardError; end

        Transaction = Struct.new(:posted_on, :description, :amount, :balance, keyword_init: true)

        HEADER_ALIASES = {
          "date" => :posted_on, "posting date" => :posted_on, "booking date" => :posted_on,
          "description" => :description, "details" => :description, "narrative" => :description,
          "amount" => :amount, "value" => :amount,
          "balance" => :balance, "running balance" => :balance
        }.freeze

        SALARY_MARKERS = /\b(salary|payroll|wages|sal\s+credit)\b/i.freeze
        DATE_FORMATS = ["%Y-%m-%d", "%d/%m/%Y", "%d.%m.%Y"].freeze

        def initialize(min_salary_amount: BigDecimal("300"))
          @min_salary_amount = min_salary_amount
        end

        # Reads every row of the export. Rows with a blank date are footer
        # or summary lines and are skipped; any other row that cannot be
        # parsed fails the whole statement with its line number.
        def parse_rows(csv_text)
          table = CSV.parse(csv_text.to_s, headers: true)
          columns = map_headers(table.headers)
          missing = %i[posted_on amount] - columns.values
          raise ParseError, "missing columns: #{missing.join(', ')}" unless missing.empty?

          transactions = []
          table.each_with_index do |row, index|
            fields = columns.each_with_object({}) { |(header, key), acc| acc[key] = row[header] }
            next if fields[:posted_on].to_s.strip.empty?

            begin
              transactions << Transaction.new(
                posted_on: parse_date(fields[:posted_on]),
                description: fields[:description].to_s.strip,
                amount: parse_amount(fields[:amount]),
                balance: fields[:balance].to_s.strip.empty? ? nil : parse_amount(fields[:balance])
              )
            rescue ArgumentError => e
              raise ParseError, "line #{index + 2}: #{e.message}"
            end
          end

          # Stable sort: same-day rows keep the bank's own ordering, which
          # the running balance column depends on.
          transactions.each_with_index.sort_by { |txn, position| [txn.posted_on, position] }.map(&:first)
        end

        # Finds credits that look like salary payments: a payroll keyword in
        # the description and an amount above the configured floor. Returns
        # at most one credit per calendar month (the largest), because a
        # bonus or an advance in the same month must not double count.
        # Credits that the bank reversed within five days (a debit of the
        # same amount with the same description) are ignored.
        def detect_salary_credits(transactions)
          debits = transactions.select { |txn| txn.amount.negative? }

          candidates = transactions.select do |txn|
            next false unless txn.amount.positive?
            next false unless txn.amount >= @min_salary_amount
            next false unless txn.description.match?(SALARY_MARKERS)

            debits.none? do |debit|
              debit.amount == -txn.amount &&
                debit.description.casecmp?(txn.description) &&
                (debit.posted_on - txn.posted_on).between?(0, 5)
            end
          end

          by_month = candidates.group_by { |txn| [txn.posted_on.year, txn.posted_on.month] }

          by_month.keys.sort.map do |month|
            by_month[month].max_by(&:amount)
          end
        end

        # Month-end balance for each calendar month covered by the
        # statement. Uses the running balance of the last transaction in the
        # month when the bank supplies one; otherwise reconstructs it from
        # the opening balance and the month's net movement.
        def monthly_balances(transactions, opening_balance:)
          return {} if transactions.empty?

          running = BigDecimal(opening_balance.to_s)
          balances = {}

          transactions.group_by { |txn| Date.new(txn.posted_on.year, txn.posted_on.month, 1) }
                      .sort_by { |month, _| month }
                      .each do |month, month_txns|
            ordered = month_txns.sort_by.with_index { |txn, position| [txn.posted_on, position] }
            net = ordered.sum(BigDecimal("0"), &:amount)
            reported = ordered.last.balance

            running = reported.nil? ? running + net : reported
            balances[month.strftime("%Y-%m")] = running
          end

          balances
        end

        # Counts calendar days on which the account closed below zero,
        # between the first and last posting date inclusive. Days without
        # transactions carry the previous day's closing balance forward.
        def overdraft_days(transactions, opening_balance:)
          return 0 if transactions.empty?

          closing_by_day = {}
          running = BigDecimal(opening_balance.to_s)
          transactions.sort_by.with_index { |txn, position| [txn.posted_on, position] }.each do |txn|
            running = txn.balance.nil? ? running + txn.amount : txn.balance
            closing_by_day[txn.posted_on] = running
          end

          first_day = closing_by_day.keys.min
          last_day = closing_by_day.keys.max
          current = BigDecimal(opening_balance.to_s)
          days = 0

          (first_day..last_day).each do |day|
            current = closing_by_day.fetch(day, current)
            days += 1 if current.negative?
          end

          days
        end

        private

        def map_headers(headers)
          headers.compact.each_with_object({}) do |header, acc|
            key = HEADER_ALIASES[header.to_s.strip.downcase]
            acc[header] = key if key
          end
        end

        def parse_date(value)
          text = value.to_s.strip
          DATE_FORMATS.each do |format|
            return Date.strptime(text, format)
          rescue Date::Error
            next
          end
          raise ArgumentError, "unrecognised date"
        end

        def parse_amount(value)
          text = value.to_s.strip.delete(" ")
          negative = text.start_with?("(") && text.end_with?(")")
          text = text.delete("()").delete(",")
          raise ArgumentError, "unrecognised amount" unless text.match?(/\A[-+]?\d+(\.\d+)?\z/)

          amount = BigDecimal(text)
          negative ? -amount : amount
        end
      end
    end
  end
end

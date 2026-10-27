require "bigdecimal"
require "csv"
require "date"
require_relative "statement_source"

module DocIntake
  module Part4
    # Turns a stored bank statement export into a list of normalised
    # transactions and per-month cash-flow totals used by the
    # affordability summary.
    class StatementLedgerParser
      class UnreadableStatement < StandardError; end

      Transaction = Struct.new(:posted_on, :description, :amount_cents, :balance_cents, keyword_init: true) do
        def credit?
          amount_cents.positive?
        end

        def debit?
          amount_cents.negative?
        end
      end

      DATE_FORMATS = ["%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%d %b %Y"].freeze
      AMOUNT_COLUMNS = %w[amount Amount AMOUNT].freeze
      CREDIT_COLUMNS = %w[credit Credit CREDIT].freeze
      DEBIT_COLUMNS = %w[debit Debit DEBIT].freeze

      def initialize(source:, logger:)
        @source = source
        @logger = logger
      end

      # Yields each transaction of one statement. The underlying file handle
      # is owned here and always released, whether the block finishes,
      # breaks early or raises.
      def each_transaction(application_id, document_id)
        return enum_for(:each_transaction, application_id, document_id) unless block_given?

        io = @source.open_statement(application_id, document_id)
        begin
          csv = CSV.new(io, headers: true, return_headers: false)
          csv.each_with_index do |row, index|
            transaction = build_transaction(row)
            if transaction.nil?
              @logger.debug("skipping statement row document_id=#{document_id} row=#{index + 2}")
              next
            end
            yield transaction
          end
        rescue CSV::MalformedCSVError => e
          raise UnreadableStatement, "malformed statement document_id=#{document_id} (#{e.message})"
        ensure
          io.close unless io.closed?
        end
      end

      def transactions(application_id, document_id)
        each_transaction(application_id, document_id).to_a
      end

      # Groups credits and debits by calendar month. Months with no
      # activity between the first and last posting are filled with zeros so
      # downstream averages are not inflated by gaps.
      def monthly_totals(transactions)
        return [] if transactions.empty?

        buckets = Hash.new { |hash, key| hash[key] = { credits_cents: 0, debits_cents: 0, count: 0 } }
        transactions.each do |txn|
          key = [txn.posted_on.year, txn.posted_on.month]
          bucket = buckets[key]
          if txn.credit?
            bucket[:credits_cents] += txn.amount_cents
          else
            bucket[:debits_cents] += txn.amount_cents.abs
          end
          bucket[:count] += 1
        end

        first = transactions.map(&:posted_on).min
        last = transactions.map(&:posted_on).max
        month = Date.new(first.year, first.month, 1)
        stop = Date.new(last.year, last.month, 1)
        totals = []
        while month <= stop
          bucket = buckets.fetch([month.year, month.month], { credits_cents: 0, debits_cents: 0, count: 0 })
          totals << bucket.merge(month: month.strftime("%Y-%m"),
                                 net_cents: bucket[:credits_cents] - bucket[:debits_cents])
          month = month.next_month
        end
        totals
      end

      private

      def build_transaction(row)
        posted_on = parse_date(row["Date"] || row["date"] || row["DATE"])
        amount_cents = signed_amount_cents(row)
        return nil if posted_on.nil? || amount_cents.nil?

        Transaction.new(
          posted_on: posted_on,
          description: (row["Description"] || row["description"]).to_s.strip,
          amount_cents: amount_cents,
          balance_cents: cents_or_nil(row["Balance"] || row["balance"])
        )
      end

      def parse_date(value)
        text = value.to_s.strip
        return nil if text.empty?

        DATE_FORMATS.each do |format|
          return Date.strptime(text, format)
        rescue Date::Error
          next
        end
        nil
      end

      def signed_amount_cents(row)
        amount_key = AMOUNT_COLUMNS.find { |name| row.has_key?(name) }
        return cents_or_nil(row[amount_key]) if amount_key

        credit = cents_or_nil(row[CREDIT_COLUMNS.find { |name| row.has_key?(name) }])
        debit = cents_or_nil(row[DEBIT_COLUMNS.find { |name| row.has_key?(name) }])
        return nil if credit.nil? && debit.nil?

        credit.to_i - debit.to_i.abs
      end

      def cents_or_nil(value)
        text = value.to_s.strip.delete(",")
        return nil if text.empty?

        negative = text.start_with?("(") && text.end_with?(")")
        text = text.delete("()")
        cents = (BigDecimal(text) * 100).round.to_i
        negative ? -cents : cents
      rescue ArgumentError
        nil
      end
    end
  end
end

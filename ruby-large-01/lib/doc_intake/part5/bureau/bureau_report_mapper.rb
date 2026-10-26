require "date"
require "bigdecimal"

module DocIntake
  module Part5
    module Bureau
      # Maps the raw bureau report payload (already JSON-decoded) into the
      # compact credit summary that is stored with the pack and forwarded
      # to risk-service. Only account-level facts are kept; the bureau's
      # identity block is dropped on purpose.
      class BureauReportMapper
        Tradeline = Struct.new(
          :account_ref, :type, :opened_on, :limit, :balance, :status, :worst_delinquency_days,
          keyword_init: true
        )

        REVOLVING_TYPES = %w[credit_card overdraft revolving_line].freeze
        CLOSED_STATUSES = %w[closed settled transferred].freeze
        DELINQUENCY_BUCKETS = [30, 60, 90].freeze

        def initialize(as_of:)
          @as_of = as_of
        end

        # Converts the bureau's account list into Tradeline structs. Accounts
        # the bureau marks as duplicates, and accounts with no reference, are
        # skipped. Amounts arrive as strings in minor units.
        def map_tradelines(report)
          accounts = Array(report.dig("credit_file", "accounts"))
          seen = {}

          accounts.each_with_object([]) do |account, lines|
            ref = account["account_ref"].to_s
            next if ref.empty?
            next if account["duplicate_of"]
            next if seen[ref]

            seen[ref] = true
            history = Array(account["payment_history"])
            worst = history.map { |entry| entry["days_past_due"].to_i }.max || 0

            lines << Tradeline.new(
              account_ref: ref,
              type: account["type"].to_s,
              opened_on: account["opened_on"] && Date.iso8601(account["opened_on"]),
              limit: minor_to_major(account["credit_limit"]),
              balance: minor_to_major(account["current_balance"]) || BigDecimal("0"),
              status: account["status"].to_s.downcase,
              worst_delinquency_days: worst
            )
          end
        end

        # Counts tradelines (open or closed) by their worst delinquency
        # bucket over the last 24 months. Each tradeline is counted once, in
        # the highest bucket it reached.
        def summarize_delinquency(report)
          cutoff = @as_of << 24
          counts = DELINQUENCY_BUCKETS.to_h { |bucket| ["dpd_#{bucket}_plus", 0] }

          Array(report.dig("credit_file", "accounts")).each do |account|
            next if account["duplicate_of"]

            recent = Array(account["payment_history"]).select do |entry|
              month = entry["month"] && Date.iso8601("#{entry['month']}-01")
              month && month >= cutoff
            end
            worst = recent.map { |entry| entry["days_past_due"].to_i }.max.to_i

            bucket = DELINQUENCY_BUCKETS.reverse.find { |threshold| worst >= threshold }
            counts["dpd_#{bucket}_plus"] += 1 if bucket
          end

          counts
        end

        # Revolving utilisation across open revolving tradelines with a known,
        # positive limit. Returns nil when there is no such tradeline rather
        # than reporting a misleading 0%.
        def revolving_utilization(tradelines)
          revolving = tradelines.select do |line|
            REVOLVING_TYPES.include?(line.type) &&
              !CLOSED_STATUSES.include?(line.status) &&
              line.limit &&
              line.limit.positive?
          end
          return nil if revolving.empty?

          total_limit = revolving.sum(BigDecimal("0"), &:limit)
          total_balance = revolving.sum(BigDecimal("0")) do |line|
            line.balance.negative? ? BigDecimal("0") : line.balance
          end

          ratio = (total_balance / total_limit * 100).round(1)
          {
            open_revolving_accounts: revolving.size,
            total_limit: total_limit.round(2),
            total_balance: total_balance.round(2),
            utilization_percent: ratio
          }
        end

        def summary(report)
          tradelines = map_tradelines(report)
          {
            report_id: report["report_id"],
            as_of: @as_of.iso8601,
            open_accounts: tradelines.count { |line| !CLOSED_STATUSES.include?(line.status) },
            delinquency: summarize_delinquency(report),
            revolving: revolving_utilization(tradelines)
          }
        end

        private

        def minor_to_major(value)
          return nil if value.nil? || value.to_s.strip.empty?

          BigDecimal(value.to_s) / 100
        end
      end
    end
  end
end

require "date"

module DocIntake
  module Part4
    # Derives the underwriting indicators shown on the pack review screen
    # from a report normalised by BureauReportFetcher.
    class BureauReportSummary
      UTILISATION_BANDS = [
        [0.0, 0.3, :low],
        [0.3, 0.5, :moderate],
        [0.5, 0.75, :high],
        [0.75, Float::INFINITY, :very_high]
      ].freeze

      REVOLVING_TYPES = %w[credit_card overdraft revolving store_card].freeze
      DELINQUENT_STATUSES = %w[arrears delinquent defaulted charged_off collections].freeze

      def initialize(today: -> { Date.today })
        @today = today
      end

      # Revolving credit utilisation across open accounts, banded. Accounts
      # with no reported limit are excluded rather than treated as maxed out.
      def utilisation(report)
        revolving = Array(report["accounts"]).select do |account|
          REVOLVING_TYPES.include?(account["type"].to_s) && account["status"] == "open"
        end
        with_limits = revolving.select { |account| account["limit_cents"].to_i.positive? }

        if with_limits.empty?
          return { "ratio" => nil, "band" => :no_revolving_credit, "accounts_considered" => 0,
                   "accounts_without_limit" => revolving.size }
        end

        balance = with_limits.sum { |account| [account["balance_cents"].to_i, 0].max }
        limit = with_limits.sum { |account| account["limit_cents"].to_i }
        ratio = (balance.to_f / limit).round(4)
        band = UTILISATION_BANDS.find { |low, high, _name| ratio >= low && ratio < high }&.last

        {
          "ratio" => ratio,
          "band" => band,
          "accounts_considered" => with_limits.size,
          "accounts_without_limit" => revolving.size - with_limits.size
        }
      end

      # Counts delinquent accounts and how recently the newest one was
      # opened. Accounts with an unparseable open date still count towards
      # the total but are ignored for recency.
      def delinquency(report)
        delinquent = Array(report["accounts"]).select do |account|
          DELINQUENT_STATUSES.include?(account["status"].to_s)
        end

        opened_dates = delinquent.filter_map do |account|
          Date.iso8601(account["opened_on"].to_s)
        rescue Date::Error
          nil
        end

        newest = opened_dates.max
        months_since_newest =
          if newest
            today = @today.call
            ((today.year * 12) + today.month) - ((newest.year * 12) + newest.month)
          end

        {
          "delinquent_accounts" => delinquent.size,
          "delinquent_balance_cents" => delinquent.sum { |account| account["balance_cents"].to_i },
          "months_since_newest_delinquent_account" => months_since_newest,
          "recent" => !months_since_newest.nil? && months_since_newest < 12
        }
      end
    end
  end
end

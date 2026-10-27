require "date"

module DocIntake
  module Part5
    module Retention
      # Decides how long each borrower document is kept and which stored
      # documents are due for purge. Retention periods follow the lending
      # record-keeping schedule: documents tied to a funded loan are kept for
      # the life of the loan plus a regulatory tail; everything else expires
      # on a shorter, per-kind schedule.
      class RetentionPolicy
        DEFAULT_DAYS = {
          "bank_statement" => 400,
          "payslip" => 400,
          "id_scan" => 180,
          "bureau_report" => 90,
          "supporting_letter" => 365
        }.freeze

        FUNDED_TAIL_YEARS = 7
        LEGAL_HOLD_STATES = %w[litigation regulator_request fraud_review].freeze

        Decision = Struct.new(:document_id, :expires_on, :reason, keyword_init: true)

        def initialize(clock: -> { Date.today }, overrides: {})
          @clock = clock
          @days = DEFAULT_DAYS.merge(overrides.transform_keys(&:to_s))
        end

        # Computes the date on which a single document stops being retained.
        # Returns nil when the document is under a legal hold, because held
        # documents never expire until the hold is lifted.
        def expiry_for(document)
          return nil if LEGAL_HOLD_STATES.include?(document[:hold_state].to_s)

          uploaded_on = to_date(document.fetch(:uploaded_at))
          kind = document.fetch(:kind).to_s
          base_days = @days.fetch(kind) do
            raise ArgumentError, "no retention period configured for kind #{kind}"
          end

          base_expiry = uploaded_on + base_days
          closed_on = document[:loan_closed_on] && to_date(document[:loan_closed_on])

          if document[:loan_funded]
            # A funded loan keeps its file until the loan closes, then for
            # the regulatory tail. Open loans have no expiry yet.
            return nil if closed_on.nil?

            funded_expiry = closed_on.next_year(FUNDED_TAIL_YEARS)
            return [base_expiry, funded_expiry].max
          end

          base_expiry
        end

        # Selects the documents whose retention has elapsed as of today,
        # oldest expiry first, so a purge job can process them in a stable
        # order and resume from the last id it handled.
        def purge_candidates(documents, limit: 500)
          raise ArgumentError, "limit must be positive" unless limit.positive?

          today = @clock.call
          due = []

          documents.each do |document|
            expires_on = expiry_for(document)
            next if expires_on.nil?
            next unless expires_on <= today

            due << Decision.new(
              document_id: document.fetch(:id),
              expires_on: expires_on,
              reason: document[:loan_funded] ? "funded_tail_elapsed" : "kind_period_elapsed"
            )
          end

          due.sort_by { |decision| [decision.expires_on, decision.document_id] }.first(limit)
        end

        def period_days(kind)
          @days.fetch(kind.to_s)
        end

        private

        def to_date(value)
          case value
          when Date then value
          when Time then value.to_date
          when String then Date.iso8601(value)
          else
            raise ArgumentError, "unsupported date value #{value.class}"
          end
        end
      end
    end
  end
end

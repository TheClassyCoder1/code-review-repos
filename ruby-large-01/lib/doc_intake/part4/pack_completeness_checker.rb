require "date"

module DocIntake
  module Part4
    # Works out whether an application's uploaded documents satisfy the
    # pack requirements for its product, and what the borrower still needs
    # to send. Requirements are evaluated against accepted documents only.
    class PackCompletenessChecker
      Requirement = Struct.new(:kind, :count, :max_age_days, :covers_months, keyword_init: true)

      REQUIREMENTS = {
        "personal_loan" => [
          Requirement.new(kind: :identity, count: 1),
          Requirement.new(kind: :payslip, count: 2, max_age_days: 62),
          Requirement.new(kind: :bank_statement, count: 1, max_age_days: 45, covers_months: 3)
        ],
        "secured_loan" => [
          Requirement.new(kind: :identity, count: 1),
          Requirement.new(kind: :proof_of_address, count: 1, max_age_days: 90),
          Requirement.new(kind: :payslip, count: 3, max_age_days: 93),
          Requirement.new(kind: :bank_statement, count: 1, max_age_days: 45, covers_months: 6)
        ]
      }.freeze

      def initialize(today: -> { Date.today })
        @today = today
      end

      # Returns one entry per unmet requirement with the number of
      # additional documents needed and the reason it is unmet.
      def missing_requirements(product, documents)
        requirements = REQUIREMENTS.fetch(product.to_s) do
          raise ArgumentError, "no pack requirements for product #{product.inspect}"
        end
        today = @today.call
        accepted = documents.select { |doc| doc[:status] == :accepted }

        requirements.filter_map do |requirement|
          of_kind = accepted.select { |doc| doc[:kind] == requirement.kind }
          fresh = of_kind.select { |doc| fresh?(doc, requirement, today) }

          if fresh.size < requirement.count
            reason = of_kind.size > fresh.size ? :too_old : :not_provided
            next({ kind: requirement.kind, needed: requirement.count - fresh.size, reason: reason })
          end

          if requirement.covers_months && covered_months(fresh) < requirement.covers_months
            next({ kind: requirement.kind, needed: 1, reason: :insufficient_history })
          end

          nil
        end
      end

      def report(product, documents)
        missing = missing_requirements(product, documents)
        {
          product: product.to_s,
          complete: missing.empty?,
          missing: missing,
          accepted_documents: documents.count { |doc| doc[:status] == :accepted },
          pending_documents: documents.count { |doc| doc[:status] == :pending_review }
        }
      end

      private

      def fresh?(doc, requirement, today)
        return true if requirement.max_age_days.nil?

        issued_on = doc[:issued_on]
        return false if issued_on.nil?

        (today - issued_on).to_i <= requirement.max_age_days
      end

      # Distinct calendar months spanned by the statements' coverage periods.
      def covered_months(statements)
        months = statements.flat_map do |doc|
          from = doc[:period_start]
          to = doc[:period_end]
          next [] if from.nil? || to.nil? || to < from

          cursor = Date.new(from.year, from.month, 1)
          span = []
          while cursor <= to
            span << [cursor.year, cursor.month]
            cursor = cursor.next_month
          end
          span
        end
        months.uniq.size
      end
    end
  end
end

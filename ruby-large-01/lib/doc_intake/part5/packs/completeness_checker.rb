module DocIntake
  module Part5
    module Packs
      # Checks whether a loan application's document pack contains everything
      # underwriting needs before it can be handed to loan-service. The
      # requirements depend on the product and on the employment type the
      # applicant declared.
      class CompletenessChecker
        Requirement = Struct.new(:kind, :min_count, :max_age_days, keyword_init: true)

        BASE_REQUIREMENTS = [
          Requirement.new(kind: "id_scan", min_count: 1, max_age_days: nil),
          Requirement.new(kind: "bank_statement", min_count: 3, max_age_days: 120)
        ].freeze

        EMPLOYMENT_REQUIREMENTS = {
          "salaried" => [Requirement.new(kind: "payslip", min_count: 2, max_age_days: 90)],
          "self_employed" => [
            Requirement.new(kind: "tax_return", min_count: 2, max_age_days: 730),
            Requirement.new(kind: "bank_statement", min_count: 6, max_age_days: 210)
          ],
          "retired" => [Requirement.new(kind: "pension_letter", min_count: 1, max_age_days: 365)]
        }.freeze

        PRODUCT_REQUIREMENTS = {
          "secured_auto" => [Requirement.new(kind: "vehicle_invoice", min_count: 1, max_age_days: 60)],
          "home_improvement" => [Requirement.new(kind: "contractor_quote", min_count: 1, max_age_days: 90)]
        }.freeze

        Gap = Struct.new(:kind, :required, :present, keyword_init: true)

        def initialize(today:)
          @today = today
        end

        def requirements_for(product:, employment_type:)
          combined = BASE_REQUIREMENTS +
                     EMPLOYMENT_REQUIREMENTS.fetch(employment_type.to_s, []) +
                     PRODUCT_REQUIREMENTS.fetch(product.to_s, [])

          # When the same kind appears more than once, the strictest count
          # and the shortest freshness window both apply.
          combined.group_by(&:kind).map do |kind, reqs|
            ages = reqs.map(&:max_age_days).compact
            Requirement.new(
              kind: kind,
              min_count: reqs.map(&:min_count).max,
              max_age_days: ages.empty? ? nil : ages.min
            )
          end
        end

        # Lists every requirement the pack does not yet satisfy. Documents
        # that failed scanning or are older than the requirement's freshness
        # window do not count towards the minimum.
        def missing_requirements(pack)
          documents = Array(pack[:documents]).reject { |doc| doc[:scan_status].to_s == "rejected" }
          requirements = requirements_for(
            product: pack.fetch(:product),
            employment_type: pack.fetch(:employment_type)
          )

          requirements.each_with_object([]) do |requirement, gaps|
            usable = documents.count do |doc|
              next false unless doc[:kind].to_s == requirement.kind
              next true if requirement.max_age_days.nil?

              issued_on = doc[:issued_on]
              next false if issued_on.nil?

              (@today - issued_on).to_i <= requirement.max_age_days
            end

            next if usable >= requirement.min_count

            gaps << Gap.new(kind: requirement.kind, required: requirement.min_count, present: usable)
          end
        end

        # Builds the short status block shown to borrowers and case workers.
        # It deliberately carries only kinds and counts, never file names,
        # because file names often contain the borrower's own name.
        def summary(pack)
          gaps = missing_requirements(pack)
          total_required = requirements_for(
            product: pack.fetch(:product),
            employment_type: pack.fetch(:employment_type)
          ).sum(&:min_count)

          outstanding = gaps.sum { |gap| gap.required - gap.present }
          satisfied = total_required - outstanding
          percent = total_required.zero? ? 100 : ((satisfied * 100.0) / total_required).floor

          {
            pack_id: pack.fetch(:id),
            complete: gaps.empty?,
            percent_complete: percent,
            outstanding: gaps.map do |gap|
              { kind: gap.kind, still_needed: gap.required - gap.present }
            end
          }
        end
      end
    end
  end
end

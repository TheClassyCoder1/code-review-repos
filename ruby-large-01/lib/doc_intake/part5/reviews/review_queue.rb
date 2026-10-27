require "time"

module DocIntake
  module Part5
    module Reviews
      # Manual review queue for packs the automated checks could not clear
      # (unreadable scans, income mismatches, suspected tampering). Cases
      # are prioritised by SLA risk and handed to reviewers with matching
      # skills, never more than a reviewer's capacity.
      class ReviewQueue
        Case = Struct.new(:id, :application_id, :reason, :opened_at, :sla_hours, :skills_required,
                          keyword_init: true)
        Reviewer = Struct.new(:id, :skills, :capacity, :open_cases, keyword_init: true)

        REASON_WEIGHTS = {
          "suspected_tampering" => 3,
          "income_mismatch" => 2,
          "unreadable_scan" => 1
        }.freeze
        DEFAULT_SLA_HOURS = 24

        def initialize(clock: -> { Time.now.utc })
          @clock = clock
        end

        # Orders open cases so that the ones closest to breaching their SLA
        # come first. Already-breached cases sort ahead of everything else,
        # oldest breach first; ties fall back to the reason weight and then
        # the case id so the order is stable between runs.
        def prioritize(cases)
          now = @clock.call

          cases.sort_by do |review_case|
            sla_seconds = (review_case.sla_hours || DEFAULT_SLA_HOURS) * 3600
            deadline = review_case.opened_at + sla_seconds
            remaining = deadline - now
            breached = remaining.negative? ? 0 : 1
            weight = REASON_WEIGHTS.fetch(review_case.reason, 0)

            [
              breached,
              remaining,
              -weight,
              review_case.id.to_s
            ]
          end
        end

        # Assigns cases, in priority order, to the eligible reviewer with the
        # fewest open cases. A reviewer is eligible when they hold every skill
        # the case requires and still have spare capacity. Cases nobody can
        # take stay unassigned and are returned separately.
        def assign(cases, reviewers)
          load = reviewers.to_h { |reviewer| [reviewer.id, reviewer.open_cases.to_i] }
          assignments = []
          unassigned = []

          prioritize(cases).each do |review_case|
            required = Array(review_case.skills_required)
            eligible = reviewers.select do |reviewer|
              (required - Array(reviewer.skills)).empty? &&
                load.fetch(reviewer.id) < reviewer.capacity.to_i
            end

            if eligible.empty?
              unassigned << review_case.id
              next
            end

            chosen = eligible.min_by { |reviewer| [load.fetch(reviewer.id), reviewer.id.to_s] }
            load[chosen.id] += 1
            assignments << { case_id: review_case.id, reviewer_id: chosen.id }
          end

          { assignments: assignments, unassigned: unassigned }
        end

        # Aggregate view for the operations dashboard: open counts per
        # reason, how many cases have breached their SLA and the age of the
        # oldest case in hours. Contains ids and counts only.
        def dashboard(cases)
          now = @clock.call
          by_reason = Hash.new(0)
          breached = 0
          oldest_hours = 0.0

          cases.each do |review_case|
            by_reason[review_case.reason] += 1

            age_hours = (now - review_case.opened_at) / 3600.0
            oldest_hours = age_hours if age_hours > oldest_hours

            sla = review_case.sla_hours || DEFAULT_SLA_HOURS
            breached += 1 if age_hours > sla
          end

          {
            open_cases: cases.size,
            by_reason: by_reason.sort.to_h,
            breached: breached,
            oldest_case_hours: oldest_hours.round(1)
          }
        end
      end
    end
  end
end

require "set"

module DocIntake
  module Part4
    # Orders the manual document-review queue. Reviewers work from the top,
    # so the score favours applications close to their decision deadline,
    # larger loans and documents the classifier was unsure about.
    class ReviewQueuePrioritizer
      HOUR = 3600
      DEADLINE_WINDOW_HOURS = 72
      AMOUNT_CAP_CENTS = 50_000_00
      WEIGHTS = { deadline: 0.5, amount: 0.2, uncertainty: 0.2, age: 0.1 }.freeze

      ReviewItem = Struct.new(:document_id, :application_id, :reviewer_id, :score, keyword_init: true)

      def initialize(clock: -> { Time.now.utc })
        @clock = clock
      end

      # Scores one queued document between 0.0 and 1.0. Items whose decision
      # deadline has already passed always score 1.0.
      def priority_score(item, now: @clock.call)
        deadline = item.fetch(:decision_due_at)
        hours_left = (deadline - now) / HOUR
        return 1.0 if hours_left <= 0

        deadline_factor = 1.0 - [hours_left / DEADLINE_WINDOW_HOURS, 1.0].min
        amount_factor = [item.fetch(:loan_amount_cents).to_f / AMOUNT_CAP_CENTS, 1.0].min
        confidence = item.fetch(:classifier_confidence, 1.0).to_f.clamp(0.0, 1.0)
        uncertainty_factor = 1.0 - confidence
        waited_hours = [(now - item.fetch(:queued_at)) / HOUR, 0].max
        age_factor = [waited_hours / 24.0, 1.0].min

        score = (WEIGHTS[:deadline] * deadline_factor) +
                (WEIGHTS[:amount] * amount_factor) +
                (WEIGHTS[:uncertainty] * uncertainty_factor) +
                (WEIGHTS[:age] * age_factor)
        score.round(4)
      end

      # Picks the next documents for a reviewer. Documents from the same
      # application stay together so one reviewer sees the whole pack, and a
      # reviewer is never handed an application they submitted or already
      # reviewed at a previous stage.
      def next_batch(queue, reviewer_id:, excluded_application_ids: [], limit: 10)
        raise ArgumentError, "limit must be positive" unless limit.positive?

        now = @clock.call
        excluded = excluded_application_ids.map(&:to_s).to_set
        eligible = queue.reject do |item|
          excluded.include?(item.fetch(:application_id).to_s) || item[:assigned_to]
        end

        groups = eligible.group_by { |item| item.fetch(:application_id) }
        ranked = groups.map do |application_id, items|
          [application_id, items, items.map { |item| priority_score(item, now: now) }.max]
        end
        ranked.sort_by! { |application_id, _items, score| [-score, application_id.to_s] }

        batch = []
        ranked.each do |_application_id, items, score|
          break if batch.size >= limit
          next if batch.any? && batch.size + items.size > limit

          items.sort_by { |item| item.fetch(:queued_at) }.each do |item|
            batch << ReviewItem.new(document_id: item.fetch(:document_id), application_id: item.fetch(:application_id),
                                    reviewer_id: reviewer_id, score: score)
          end
        end
        batch
      end
    end
  end
end

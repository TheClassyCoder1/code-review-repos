module DocIntake
  module Part3
    # Manual review queue for document packs that automated checks could not
    # clear. Tasks move queued -> claimed -> decided.
    class ReviewQueue
      DECISIONS = %w[approved rejected needs_resubmission].freeze

      def initialize(db, reviewer_lock: ReviewerLock.new(db), clock: -> { Time.now.utc })
        @db = db
        @reviewer_lock = reviewer_lock
        @clock = clock
      end

      def enqueue(pack_id)
        @db[:review_tasks].insert(pack_id: pack_id, state: "queued", created_at: @clock.call)
      end

      def next_queued(limit: 20)
        @db[:review_tasks].where(state: "queued").order(:created_at, :id).limit(limit).all
      end

      # A reviewer picks a task from the queue.
      def claim(task_id, reviewer_id)
        task = @db[:review_tasks].where(id: task_id).first
        raise NotFound, "review task #{task_id} not found" unless task
        raise Conflict, "review task #{task_id} already #{task[:state]}" unless task[:state] == "queued"

        @db[:review_tasks].where(id: task_id).update(
          state: "claimed",
          assignee_id: reviewer_id,
          claimed_at: @clock.call
        )
        @reviewer_lock.hold(reviewer_id) do |reviewer|
          @db[:reviewers].where(id: reviewer_id).update(open_tasks: reviewer[:open_tasks] + 1)
        end
        Part3.logger.info("review task=#{task_id} claimed reviewer=#{reviewer_id}")
        task_id
      end

      # Hands the oldest queued task for a team to whoever has the most room.
      def auto_assign(team)
        task = next_queued(limit: 1).first
        return nil unless task

        @reviewer_lock.hold_least_loaded(team) do |reviewer|
          updated = @db[:review_tasks].where(id: task[:id], state: "queued")
                                      .update(state: "claimed", assignee_id: reviewer[:id], claimed_at: @clock.call)
          return nil if updated.zero?

          @db[:reviewers].where(id: reviewer[:id]).update(open_tasks: reviewer[:open_tasks] + 1)
          reviewer[:id]
        end
      end

      def decide(task_id, decision)
        raise ArgumentError, "unknown decision #{decision}" unless DECISIONS.include?(decision)

        task = @db[:review_tasks].where(id: task_id, state: "claimed").first
        raise Conflict, "review task #{task_id} is not claimed" unless task

        @db[:review_tasks].where(id: task_id).update(state: "decided", decision: decision)
        release(task[:assignee_id]) if task[:assignee_id]
        Part3.logger.info("review task=#{task_id} decided=#{decision}")
      end

      # Builds the card shown in the reviewer inbox.
      def describe(task_id)
        task = @db[:review_tasks].where(id: task_id).first
        raise NotFound, "review task #{task_id} not found" unless task

        pack = @db[:document_packs].where(id: task[:pack_id]).first
        {
          task_id: task[:id],
          state: task[:state],
          pack_id: pack[:id],
          loan_application_id: pack[:loan_application_id],
          document_count: pack[:document_count],
          waiting_since: task[:created_at]
        }
      end

      private

      def release(reviewer_id)
        @reviewer_lock.hold(reviewer_id) do |reviewer|
          remaining = [reviewer[:open_tasks] - 1, 0].max
          @db[:reviewers].where(id: reviewer_id).update(open_tasks: remaining)
        end
      end
    end
  end
end

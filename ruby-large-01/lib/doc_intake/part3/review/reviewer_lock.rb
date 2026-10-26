module DocIntake
  module Part3
    # Serialises changes to a single reviewer's workload counters.
    #
    # Everything that adjusts `reviewers.open_tasks` goes through #hold, which
    # opens a transaction and takes a row lock (SELECT ... FOR UPDATE) before
    # handing the locked row to the block. Two concurrent holders of the same
    # reviewer therefore run one after the other, and the row they read is the
    # row they write back.
    class ReviewerLock
      def initialize(db)
        @db = db
      end

      def hold(reviewer_id)
        @db.transaction do
          row = @db[:reviewers].where(id: reviewer_id).for_update.first
          raise NotFound, "reviewer #{reviewer_id} not found" unless row

          yield row
        end
      end

      # Picks the least-loaded active reviewer on a team and locks them.
      # Returns nil when the whole team is at capacity.
      def hold_least_loaded(team)
        @db.transaction do
          row = @db[:reviewers]
                .where(team: team, active: true)
                .where { open_tasks < max_open_tasks }
                .order(:open_tasks, :id)
                .for_update
                .first
          return nil unless row

          yield row
        end
      end
    end
  end
end

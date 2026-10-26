require "fileutils"

module DocIntake
  module Part3
    # Deletes stored documents for packs whose loan application was rejected
    # or withdrawn long enough ago. The pack row itself is kept (with
    # purged_at set) so loan-service can still resolve the pack id.
    class RetentionSweeper
      STATUSES = %w[rejected withdrawn].freeze

      def initialize(db, config:, storage_root:, clock: -> { Time.now.utc })
        @db = db
        @config = config
        @storage_root = storage_root
        @clock = clock
      end

      def sweep
        now = @clock.call
        STATUSES.sum { |status| sweep_status(status, @config.cutoff_for(status, now)) }
      end

      private

      def sweep_status(status, cutoff)
        candidates = @db[:document_packs]
                     .where(status: status, purged_at: nil)
                     .where { closed_at < cutoff }
                     .order(:id)
        total = candidates.count
        scanned = 0
        last_id = 0
        purged = 0

        while scanned < total
          ids = candidates.where { id > last_id }.limit(@config.batch_size).select_map(:id)
          ids.each { |id| purged += 1 if purge(id) }
          last_id = ids.last if ids.any?
          scanned += @config.batch_size
        end
        Part3.logger.info("retention status=#{status} candidates=#{total} purged=#{purged}")
        purged
      end

      def purge(pack_id)
        return true if @config.dry_run

        FileUtils.rm_rf(File.join(@storage_root, pack_id.to_s))
        @db[:document_packs].where(id: pack_id).update(purged_at: @clock.call)
        true
      rescue SystemCallError => e
        Part3.logger.error("retention purge failed pack=#{pack_id} error=#{e.class}")
        false
      end
    end
  end
end

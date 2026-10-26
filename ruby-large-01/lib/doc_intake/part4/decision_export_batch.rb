require "date"
require "fileutils"
require_relative "decision_export_writer"

module DocIntake
  module Part4
    # Nightly job that exports decision-ready applications. The cron entry
    # runs on every intake node, so the whole batch for a feed date runs
    # under an exclusive flock on a lock file in the shared export volume;
    # only one node at a time can check for and create export files.
    class DecisionExportBatch
      class LockUnavailable < StandardError; end

      LOCK_FILE = ".decision-export.lock".freeze

      Result = Struct.new(:feed_date, :written, :skipped, :failed, keyword_init: true)

      def initialize(writer:, export_root:, export_source:, logger:)
        @writer = writer
        @export_root = File.expand_path(export_root)
        @export_source = export_source
        @logger = logger
      end

      def run(feed_date = Date.today, wait: true)
        with_feed_lock(wait: wait) do
          result = Result.new(feed_date: feed_date, written: 0, skipped: 0, failed: [])
          @export_source.ready_for_export(feed_date).each do |export|
            case @writer.write_once(feed_date, export)
            when :written then result.written += 1
            when :already_exported then result.skipped += 1
            end
          rescue KeyError, SystemCallError => e
            result.failed << export[:application_id]
            @logger.error("decision export failed application_id=#{export[:application_id]} error=#{e.class}")
          end
          @logger.info("decision export feed=#{feed_date} written=#{result.written} " \
                       "skipped=#{result.skipped} failed=#{result.failed.size}")
          result
        end
      end

      private

      def with_feed_lock(wait:)
        FileUtils.mkdir_p(@export_root)
        File.open(File.join(@export_root, LOCK_FILE), File::RDWR | File::CREAT, 0o644) do |lock|
          mode = wait ? File::LOCK_EX : (File::LOCK_EX | File::LOCK_NB)
          raise LockUnavailable, "decision export already running" unless lock.flock(mode)

          begin
            yield
          ensure
            lock.flock(File::LOCK_UN)
          end
        end
      end
    end
  end
end

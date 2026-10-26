module DocIntake
  module Part3
    # Retention policy for stored borrower documents. Loaded and validated
    # once at boot; the sweeper only ever receives a validated instance.
    class RetentionConfig
      class Invalid < Error; end

      BATCH_RANGE = (1..5_000).freeze

      attr_reader :rejected_pack_days, :withdrawn_pack_days, :batch_size, :dry_run

      def self.load!(source = ENV)
        new(
          rejected_pack_days: Integer(source.fetch("INTAKE_RETAIN_REJECTED_DAYS", "90")),
          withdrawn_pack_days: Integer(source.fetch("INTAKE_RETAIN_WITHDRAWN_DAYS", "30")),
          batch_size: Integer(source.fetch("INTAKE_RETENTION_BATCH", "500")),
          dry_run: source.fetch("INTAKE_RETENTION_DRY_RUN", "false") == "true"
        )
      end

      def initialize(rejected_pack_days:, withdrawn_pack_days:, batch_size:, dry_run: false)
        @rejected_pack_days = rejected_pack_days
        @withdrawn_pack_days = withdrawn_pack_days
        @batch_size = batch_size
        @dry_run = dry_run
        validate!
        freeze
      end

      def cutoff_for(status, now)
        days = status == "rejected" ? rejected_pack_days : withdrawn_pack_days
        now - (days * 86_400)
      end

      private

      def validate!
        raise Invalid, "retention days must be at least 1" unless rejected_pack_days >= 1 && withdrawn_pack_days >= 1
        # The sweeper advances its cursor by batch_size; anything outside this
        # range would either stall it or let one pass lock half the table.
        raise Invalid, "retention batch must be within #{BATCH_RANGE}" unless BATCH_RANGE.cover?(batch_size)
      end
    end
  end
end

require "time"

module DocIntake
  module Part4
    # Decides when stored borrower documents may be purged. Retention depends
    # on how the application ended: declined and withdrawn applications keep
    # documents for a shorter period than funded loans, and any document
    # under a legal hold is kept regardless of age.
    class RetentionPolicy
      DAY = 86_400

      RETENTION_DAYS = {
        "funded" => 6 * 365,
        "declined" => 2 * 365,
        "withdrawn" => 365,
        "expired" => 180
      }.freeze

      OPEN_STATUSES = %w[draft submitted in_review referred].freeze

      PurgeCandidate = Struct.new(:document_id, :application_id, :reason, :eligible_since, keyword_init: true)

      def initialize(clock: -> { Time.now.utc })
        @clock = clock
      end

      def retention_days_for(application_status)
        RETENTION_DAYS.fetch(application_status.to_s) do
          raise ArgumentError, "no retention rule for status #{application_status.inspect}"
        end
      end

      # Returns the documents that are past retention, oldest first. Each
      # document row needs :id, :application_id, :application_status,
      # :closed_at (Time or nil) and :legal_hold.
      def purge_candidates(documents)
        now = @clock.call
        candidates = documents.filter_map do |doc|
          next if doc[:legal_hold]

          status = doc.fetch(:application_status).to_s
          next if OPEN_STATUSES.include?(status)

          closed_at = doc[:closed_at]
          next if closed_at.nil?

          eligible_since = closed_at + (retention_days_for(status) * DAY)
          next if eligible_since > now

          PurgeCandidate.new(
            document_id: doc.fetch(:id),
            application_id: doc.fetch(:application_id),
            reason: "retention_#{status}",
            eligible_since: eligible_since
          )
        end
        candidates.sort_by { |candidate| [candidate.eligible_since, candidate.document_id.to_s] }
      end

      # Splits candidates into purge batches that never mix applications
      # across a batch boundary, so a crash mid-run leaves each application
      # either fully purged or untouched by the interrupted batch.
      def batches(candidates, max_batch_size: 500)
        raise ArgumentError, "max_batch_size must be positive" unless max_batch_size.positive?

        by_application = candidates.group_by(&:application_id)
        batches = []
        current = []
        by_application.each_value do |group|
          if current.any? && current.size + group.size > max_batch_size
            batches << current
            current = []
          end
          current.concat(group)
          next unless current.size >= max_batch_size

          batches << current
          current = []
        end
        batches << current if current.any?
        batches
      end
    end
  end
end

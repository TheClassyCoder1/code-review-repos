require "time"

module DocIntake
  module Part7
    # A request from an underwriter asking the borrower to upload a fresh copy
    # of one document in an application pack (blurry scan, expired payslip,
    # statement missing a page, ...).
    #
    # Instances are plain value objects; persistence lives in
    # ResubmissionRepository so the object can be built from a row or from a
    # JSON payload without touching the database.
    class ResubmissionRequest
      STATUSES = %w[open fulfilled cancelled expired].freeze
      DOCUMENT_KINDS = %w[bank_statement payslip id_scan proof_of_address tax_return].freeze
      REASONS = %w[illegible incomplete outdated wrong_document other].freeze

      attr_reader :id, :application_id, :pack_id, :document_kind, :reason,
                  :note, :status, :requested_by, :requested_at, :due_at,
                  :fulfilled_document_id, :fulfilled_at, :reminder_count,
                  :last_reminded_at

      def self.from_row(row)
        new(
          id: row[:id],
          application_id: row[:application_id],
          pack_id: row[:pack_id],
          document_kind: row[:document_kind],
          reason: row[:reason],
          note: row[:note],
          status: row[:status],
          requested_by: row[:requested_by],
          requested_at: row[:requested_at],
          due_at: row[:due_at],
          fulfilled_document_id: row[:fulfilled_document_id],
          fulfilled_at: row[:fulfilled_at],
          reminder_count: row[:reminder_count] || 0,
          last_reminded_at: row[:last_reminded_at]
        )
      end

      def initialize(attrs)
        @id = attrs[:id]
        @application_id = attrs.fetch(:application_id)
        @pack_id = attrs.fetch(:pack_id)
        @document_kind = attrs.fetch(:document_kind).to_s
        @reason = (attrs[:reason] || "other").to_s
        @note = attrs[:note]
        @status = (attrs[:status] || "open").to_s
        @requested_by = attrs[:requested_by]
        @requested_at = coerce_time(attrs[:requested_at]) || Time.now.utc
        @due_at = coerce_time(attrs[:due_at])
        @fulfilled_document_id = attrs[:fulfilled_document_id]
        @fulfilled_at = coerce_time(attrs[:fulfilled_at])
        @reminder_count = Integer(attrs[:reminder_count] || 0)
        @last_reminded_at = coerce_time(attrs[:last_reminded_at])
      end

      def open?
        status == "open"
      end

      def fulfilled?
        status == "fulfilled"
      end

      def overdue?(now = Time.now.utc)
        open? && !due_at.nil? && due_at < now
      end

      def errors
        list = []
        list << "unknown document kind" unless DOCUMENT_KINDS.include?(document_kind)
        list << "unknown reason" unless REASONS.include?(reason)
        list << "unknown status" unless STATUSES.include?(status)
        list << "note too long" if note && note.length > 500
        list << "due date must be after request date" if due_at && due_at <= requested_at
        list
      end

      def valid?
        errors.empty?
      end

      def to_row
        {
          application_id: application_id,
          pack_id: pack_id,
          document_kind: document_kind,
          reason: reason,
          note: note,
          status: status,
          requested_by: requested_by,
          requested_at: requested_at,
          due_at: due_at,
          fulfilled_document_id: fulfilled_document_id,
          fulfilled_at: fulfilled_at,
          reminder_count: reminder_count,
          last_reminded_at: last_reminded_at
        }
      end

      # Shape returned by GET /api/v1/part7/resubmissions/:id. Kept flat so
      # portal-bff can render it without a second lookup.
      def to_api
        {
          id: id,
          applicationId: application_id,
          packId: pack_id,
          documentKind: document_kind,
          reason: reason,
          note: note,
          status: status,
          requestedAt: requested_at&.iso8601,
          dueAt: due_at&.iso8601,
          fulfilledDocumentId: fulfilled_document_id,
          fulfilledAt: fulfilled_at&.iso8601,
          reminderCount: reminder_count
        }
      end

      private

      def coerce_time(value)
        case value
        when nil, "" then nil
        when Time then value.utc
        when String then Time.iso8601(value).utc
        else value.to_time.utc
        end
      end
    end
  end
end

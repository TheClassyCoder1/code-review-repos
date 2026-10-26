require_relative "resubmission_request"

module DocIntake
  module Part7
    class ResubmissionNotFound < StandardError; end

    # Sequel-backed persistence for resubmission requests.
    #
    # Expected schema (see db/part7/001_create_resubmission_requests.rb):
    #   resubmission_requests(id, application_id, pack_id, document_kind,
    #     reason, note, status, requested_by, requested_at, due_at,
    #     fulfilled_document_id, fulfilled_at, reminder_count,
    #     last_reminded_at)
    class ResubmissionRepository
      TABLE = :resubmission_requests

      def initialize(db)
        @db = db
      end

      def find(id)
        row = dataset.where(id: id).first
        raise ResubmissionNotFound, "resubmission #{id} not found" unless row

        ResubmissionRequest.from_row(row)
      end

      def find_open_for(application_id, document_kind)
        row = dataset
              .where(application_id: application_id, document_kind: document_kind.to_s, status: "open")
              .order(Sequel.desc(:requested_at))
              .first
        row && ResubmissionRequest.from_row(row)
      end

      def list_for_application(application_id)
        dataset
          .where(application_id: application_id)
          .order(Sequel.desc(:requested_at))
          .map { |row| ResubmissionRequest.from_row(row) }
      end

      def create(request)
        id = dataset.insert(request.to_row)
        find(id)
      end

      def mark_fulfilled(id, document_id, at: Time.now.utc)
        updated = dataset
                  .where(id: id, status: "open")
                  .update(status: "fulfilled", fulfilled_document_id: document_id, fulfilled_at: at)
        raise ResubmissionNotFound, "open resubmission #{id} not found" if updated.zero?

        find(id)
      end

      def cancel(id)
        updated = dataset.where(id: id, status: "open").update(status: "cancelled")
        raise ResubmissionNotFound, "open resubmission #{id} not found" if updated.zero?

        find(id)
      end

      def expire_overdue(now: Time.now.utc, grace_days: 14)
        cutoff = now - (grace_days * 86_400)
        dataset.where(status: "open").where { due_at < cutoff }.update(status: "expired")
      end

      def record_reminder(id, at: Time.now.utc)
        dataset.where(id: id).update(
          reminder_count: Sequel[:reminder_count] + 1,
          last_reminded_at: at
        )
      end

      # Open requests joined with the pack status so the reminder sweep can
      # decide which borrowers still need a nudge.
      def open_with_pack_status
        @db[TABLE]
          .join(:document_packs, id: :pack_id)
          .where(Sequel[TABLE][:status] => "open")
          .select_all(TABLE)
          .select_append(Sequel[:document_packs][:status].as(:pack_status))
          .all
      end

      def counts_by_status(application_id)
        dataset
          .where(application_id: application_id)
          .group_and_count(:status)
          .to_h { |row| [row[:status], row[:count]] }
      end

      private

      def dataset
        @db[TABLE]
      end
    end
  end
end

require "json"
require "time"

module DocIntake
  module Part2
    # Persistence for pack review state: assignment, checklist and the
    # reviewer notes thread (kept as a JSON array on the pack row).
    class PackReviewRepository
      def initialize(db)
        @db = db
      end

      def find(pack_id)
        row = @db[:packs].where(id: pack_id).first
        raise NotFound, "pack not found" if row.nil?

        row
      end

      def documents(pack_id)
        @db[:uploads].where(pack_id: pack_id).order(:created_at).all
      end

      def notes(pack_id)
        parse_notes(find(pack_id)[:notes_json])
      end

      def add_note(pack_id, reviewer_id, reviewer_handle, body)
        row = find(pack_id)
        notes = parse_notes(row[:notes_json])
        note = {
          id: "#{pack_id}-#{notes.size + 1}",
          reviewer_id: reviewer_id,
          reviewer_handle: reviewer_handle,
          body: body.to_s,
          created_at: Time.now.utc.iso8601
        }
        notes << note
        @db[:packs].where(id: pack_id).update(notes_json: JSON.generate(notes), updated_at: Time.now.utc)
        note
      end

      def assign(pack_id, reviewer_id)
        updated = @db[:packs]
                  .where(id: pack_id, status: %w[collecting in_review])
                  .update(reviewer_id: reviewer_id, status: "in_review", updated_at: Time.now.utc)
        raise NotFound, "pack not assignable" if updated.zero?

        updated
      end

      def record_checklist(pack_id, checklist, expected_version)
        updated = @db[:packs]
                  .where(id: pack_id, checklist_version: expected_version)
                  .update(checklist_json: JSON.generate(checklist),
                          checklist_version: expected_version + 1,
                          updated_at: Time.now.utc)
        raise IntegrityError, "checklist changed by another reviewer" if updated.zero?

        expected_version + 1
      end

      private

      def parse_notes(raw)
        return [] if raw.nil? || raw.empty?

        JSON.parse(raw, symbolize_names: true).map do |n|
          n.merge(created_at: n[:created_at] && Time.parse(n[:created_at]))
        end
      end
    end
  end
end

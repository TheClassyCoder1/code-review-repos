module DocIntake
  module Part3
    # Running totals per pack shown on the reviewer dashboard and returned to
    # loan-service with the pack status.
    class PackSummary
      def initialize(db, clock: -> { Time.now.utc })
        @db = db
        @clock = clock
      end

      def fetch(pack_id)
        row = @db[:pack_summaries].where(pack_id: pack_id).first
        return { pack_id: pack_id, document_count: 0, total_bytes: 0 } unless row

        { pack_id: pack_id, document_count: row[:document_count], total_bytes: row[:total_bytes] }
      end

      # Invoked from the upload receiver once a document has been stored.
      def record_document(pack_id, bytes)
        raise ArgumentError, "bytes must not be negative" if bytes.negative?

        row = ensure_row(pack_id)
        @db[:pack_summaries].where(id: row[:id]).update(
          document_count: row[:document_count] + 1,
          total_bytes: row[:total_bytes] + bytes,
          updated_at: @clock.call
        )
      end

      # Invoked when a reviewer removes a document from the pack.
      def remove_document(pack_id, bytes)
        @db[:pack_summaries]
          .where(pack_id: pack_id)
          .where { document_count > 0 }
          .update(
            document_count: Sequel[:document_count] - 1,
            total_bytes: Sequel.function(:greatest, Sequel[:total_bytes] - bytes, 0),
            updated_at: @clock.call
          )
      end

      private

      def ensure_row(pack_id)
        row = @db[:pack_summaries].where(pack_id: pack_id).first
        return row if row

        @db[:pack_summaries].insert_conflict.insert(pack_id: pack_id, updated_at: @clock.call)
        @db[:pack_summaries].where(pack_id: pack_id).first
      end
    end
  end
end

module DocIntake
  module Part3
    # Walks the OCR pages of a pack in id order, a batch at a time, so large
    # packs can be re-indexed without loading every page at once.
    class PageCursor
      DEFAULT_BATCH = 200

      def initialize(db, batch_size: DEFAULT_BATCH)
        @db = db
        @batch_size = batch_size
      end

      def each_batch(pack_id)
        return enum_for(:each_batch, pack_id) unless block_given?

        last_id = 0
        loop do
          rows = @db[:ocr_pages]
                 .where(pack_id: pack_id)
                 .where { id >= last_id }
                 .order(:id)
                 .limit(@batch_size)
                 .all
          break if rows.empty?

          yield rows
          last_id = rows.last[:id]
        end
      end

      def low_confidence_pages(pack_id, threshold: 0.6)
        flagged = []
        each_batch(pack_id) do |rows|
          rows.each do |row|
            flagged << row[:page_number] if row[:confidence] && row[:confidence] < threshold
          end
        end
        flagged
      end
    end
  end
end

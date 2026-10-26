module DocIntake
  module Part3
    # Tracks how many pages of a pack have arrived versus how many the
    # borrower declared, so a pack is only released for review once complete.
    class PageLedger
      class Incomplete < Error; end

      def initialize(db, clock: -> { Time.now.utc })
        @db = db
        @clock = clock
      end

      def expect(pack_id, pages)
        raise ArgumentError, "pages must be positive" unless pages.to_i.positive?

        ds = @db[:pack_page_ledgers].where(pack_id: pack_id)
        if ds.empty?
          @db[:pack_page_ledgers].insert(pack_id: pack_id, pages_expected: pages, updated_at: @clock.call)
        else
          ds.update(pages_expected: pages, updated_at: @clock.call)
        end
      end

      # Called by each upload worker after it has stored a batch of pages.
      # Uploads for one pack are spread across workers, so several calls for
      # the same pack can be in flight at once.
      def record_pages(pack_id, count)
        ledger = @db[:pack_page_ledgers].where(pack_id: pack_id).first
        raise NotFound, "no page ledger for pack #{pack_id}" unless ledger

        received = ledger[:pages_received] + count
        @db[:pack_page_ledgers].where(id: ledger[:id]).update(pages_received: received, updated_at: @clock.call)
        received
      end

      def complete?(pack_id)
        ledger = @db[:pack_page_ledgers].where(pack_id: pack_id).first
        !ledger.nil? && ledger[:pages_received] >= ledger[:pages_expected]
      end

      # Compares what OCR actually stored against what the ledger expects and
      # refuses to continue if any page went missing along the way. Callers
      # that ingest page-by-page rely on this to surface skipped pages.
      def reconcile!(pack_id)
        ledger = @db[:pack_page_ledgers].where(pack_id: pack_id).first
        raise NotFound, "no page ledger for pack #{pack_id}" unless ledger

        stored = @db[:ocr_pages].where(pack_id: pack_id).count
        return stored if stored == ledger[:pages_expected]

        missing = ledger[:pages_expected] - stored
        Part3.logger.error("ocr reconcile pack=#{pack_id} expected=#{ledger[:pages_expected]} stored=#{stored}")
        raise Incomplete, "pack #{pack_id} is missing #{missing} OCR page(s)"
      end
    end
  end
end

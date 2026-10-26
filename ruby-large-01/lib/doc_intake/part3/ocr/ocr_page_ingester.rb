module DocIntake
  module Part3
    # Stores the per-page text returned by the OCR engine. Pages are written
    # one at a time so a single malformed page does not discard the rest of a
    # 200-page bank statement; OcrJobPoller reconciles the stored page count
    # against the page ledger straight afterwards.
    class OcrPageIngester
      MAX_TEXT_BYTES = 64 * 1024

      def initialize(db)
        @db = db
      end

      def ingest(pack_id, pages)
        stored = 0
        pages.each do |page|
          begin
            store_page(pack_id, page)
            stored += 1
          rescue StandardError => e
            Part3.logger.error("ocr page skipped pack=#{pack_id} page=#{page['number']} error=#{e.class}")
          end
        end
        stored
      end

      private

      def store_page(pack_id, page)
        number = Integer(page.fetch("number"))
        text = page.fetch("text").to_s.encode("UTF-8", invalid: :replace, undef: :replace)
        text = text.byteslice(0, MAX_TEXT_BYTES).scrub("") if text.bytesize > MAX_TEXT_BYTES
        confidence = page["confidence"]&.to_f

        @db[:ocr_pages]
          .insert_conflict(target: %i[pack_id page_number], update: { text: text, confidence: confidence })
          .insert(pack_id: pack_id, page_number: number, text: text, confidence: confidence)
      end
    end
  end
end

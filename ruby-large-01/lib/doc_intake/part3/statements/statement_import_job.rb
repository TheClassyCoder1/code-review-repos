module DocIntake
  module Part3
    # Turns the OCR text of a bank statement pack into statement_lines rows
    # that affordability checks in loan-service read through the API.
    class StatementImportJob
      Summary = Struct.new(:pack_id, :imported, :skipped, :status, keyword_init: true)

      def initialize(db, cursor: PageCursor.new(db))
        @db = db
        @cursor = cursor
      end

      def perform(pack_id)
        summary = Summary.new(pack_id: pack_id, imported: 0, skipped: 0, status: "running")
        begin
          @db.transaction do
            @db[:statement_lines].where(pack_id: pack_id).delete
            @cursor.each_batch(pack_id) do |pages|
              rows = pages.flat_map { |page| rows_for(pack_id, page[:text].to_s, summary) }
              @db[:statement_lines].multi_insert(rows) unless rows.empty?
              summary.imported += rows.size
            end
          end
          summary.status = "imported"
        rescue Date::Error, Sequel::DatabaseError => e
          summary.status = "failed"
          Part3.logger.error("statement import failed pack=#{pack_id} error=#{e.class}")
          raise
        ensure
          Part3.logger.info("statement import pack=#{pack_id} status=#{summary.status} imported=#{summary.imported}")
          return summary
        end
      end

      private

      def rows_for(pack_id, text, summary)
        text.each_line.filter_map do |line|
          unless StatementParser.statement_line?(line)
            summary.skipped += 1
            next
          end

          entry = StatementParser.parse_line(line)
          { pack_id: pack_id, posted_on: entry.posted_on, description: entry.description, amount: entry.amount }
        end
      end
    end
  end
end

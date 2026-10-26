module DocIntake
  module Part2
    # Reviewer pack search. Supports status filtering, free-text search over
    # the pack label and paging; ordering comes from SortParams.
    class PackSearchRepository
      MAX_PAGE_SIZE = 100

      Page = Struct.new(:rows, :page, :per_page, :total, keyword_init: true)

      def initialize(db)
        @db = db
      end

      def search(sort:, status: nil, query: nil, page: 1, per_page: 25)
        per_page = per_page.to_i.clamp(1, MAX_PAGE_SIZE)
        page = [page.to_i, 1].max

        dataset = base_dataset
        dataset = dataset.where(Sequel[:packs][:status] => status) if status
        dataset = dataset.where(Sequel.ilike(Sequel[:packs][:label], "%#{escape_like(query)}%")) if query && !query.empty?

        total = dataset.count
        rows = dataset
               .order(Sequel.lit("#{sort.column} #{sort.direction} NULLS LAST"), Sequel[:packs][:id])
               .limit(per_page, (page - 1) * per_page)
               .all

        Page.new(rows: rows, page: page, per_page: per_page, total: total)
      end

      private

      def base_dataset
        @db[:packs]
          .left_join(:uploads, pack_id: :id)
          .group(Sequel[:packs][:id])
          .select_all(:packs)
          .select_append { count(Sequel[:uploads][:upload_id]).as(:document_count) }
      end

      def escape_like(value)
        value.to_s.gsub(/[\\%_]/) { |m| "\\#{m}" }
      end
    end
  end
end

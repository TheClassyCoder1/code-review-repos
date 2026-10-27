module DocIntake
  module Part3
    # Per-pack tag breakdown shown on the reviewer's pack page: which tags are
    # on the pack, who added them, and how many came from the classifier.
    class TagReport
      def initialize(db)
        @db = db
      end

      def for_pack(pack_id)
        rows = @db[:document_tags].where(pack_id: pack_id).order(:created_at, :id).all

        by_source = rows.group_by { |row| row[:source] }
        {
          pack_id: pack_id,
          total: rows.size,
          tags: rows.map { |row| { tag: row[:tag], source: row[:source], added_at: row[:created_at] } },
          by_source: by_source.transform_values(&:size),
          classifier_only: classifier_only(rows)
        }
      end

      # Tags across a set of packs, counted. Used by the weekly quality report,
      # which pages through packs itself and passes a bounded id list.
      def frequency(pack_ids)
        raise ArgumentError, "at most 500 packs per call" if pack_ids.size > 500

        @db[:document_tags]
          .where(pack_id: pack_ids)
          .group_and_count(:tag)
          .order(Sequel.desc(:count), :tag)
          .to_hash(:tag, :count)
      end

      private

      def classifier_only(rows)
        reviewer_tags = rows.select { |r| r[:source] == "reviewer" }.map { |r| r[:tag] }
        rows.select { |r| r[:source] == "classifier" && !reviewer_tags.include?(r[:tag]) }.map { |r| r[:tag] }
      end
    end
  end
end

module DocIntake
  module Part3
    # Free-form tags reviewers and classifiers attach to a pack ("payslip-mismatch",
    # "address-proof", ...). The number of tags per pack is capped so a noisy
    # classifier cannot flood a pack.
    class DocumentTagging
      MAX_TAGS_PER_PACK = Integer(ENV.fetch("INTAKE_MAX_TAGS_PER_PACK", "50"))
      TAG_FORMAT = /\A[a-z0-9][a-z0-9\-]{0,47}\z/
      SOURCES = %w[reviewer classifier import].freeze

      class LimitReached < Error; end

      def initialize(db, clock: -> { Time.now.utc })
        @db = db
        @clock = clock
      end

      def tag(pack_id, tag, source: "reviewer")
        tag = tag.to_s.strip.downcase
        raise ArgumentError, "invalid tag" unless TAG_FORMAT.match?(tag)
        raise ArgumentError, "unknown source #{source}" unless SOURCES.include?(source)

        @db.transaction do
          # Lock the pack row so concurrent taggers of the same pack queue up
          # behind each other and the count below cannot be raced past.
          pack = @db[:document_packs].where(id: pack_id).for_update.first
          raise NotFound, "pack #{pack_id} not found" unless pack

          tags = @db[:document_tags].where(pack_id: pack_id)
          return false if tags.where(tag: tag).count.positive?
          raise LimitReached, "pack #{pack_id} already has #{MAX_TAGS_PER_PACK} tags" if tags.count >= MAX_TAGS_PER_PACK

          @db[:document_tags].insert(pack_id: pack_id, tag: tag, source: source, created_at: @clock.call)
        end
        true
      end

      def untag(pack_id, tag)
        @db[:document_tags].where(pack_id: pack_id, tag: tag.to_s.downcase).delete.positive?
      end

      def tags_for(pack_id)
        @db[:document_tags].where(pack_id: pack_id).order(:tag).select_map(:tag)
      end
    end
  end
end

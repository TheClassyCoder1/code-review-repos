module DocIntake
  module Part2
    # Round-trips reviewer drafts through the browser. The portal stores the
    # sealed envelope in a hidden form field and posts it back on the next
    # autosave.
    class DraftCache
      def initialize(envelope: SignedEnvelope.new, logger: Part2.logger)
        @envelope = envelope
        @logger = logger
      end

      def pack(draft)
        @envelope.seal(DraftCodec.dump(draft))
      end

      # Returns the draft for the reviewer, or nil if the posted value is
      # missing, forged or belongs to someone else.
      def unpack(sealed, reviewer_id:, pack_id:)
        return nil if sealed.nil? || sealed.empty?

        draft = DraftCodec.load(@envelope.open(sealed))
        return nil unless draft.reviewer_id == reviewer_id && draft.pack_id == pack_id

        draft
      rescue SignedEnvelope::Tampered
        @logger.warn("draft rejected reviewer=#{reviewer_id} pack=#{pack_id}")
        nil
      end
    end
  end
end

module DocIntake
  module Part2
    # Serialises reviewer drafts (checklist state, highlighted regions and
    # unsent notes) so they survive a browser refresh. Drafts carry Time and
    # Symbol values, which is why the compact Marshal format is used rather
    # than JSON.
    module DraftCodec
      FORMAT_VERSION = 2

      Draft = Struct.new(:pack_id, :reviewer_id, :checklist, :regions, :note, :saved_at, keyword_init: true)

      module_function

      def dump(draft)
        Marshal.dump([FORMAT_VERSION, draft.to_h])
      end

      def load(bytes)
        version, attrs = Marshal.load(bytes)
        raise InvalidInput, "unsupported draft format" unless version == FORMAT_VERSION

        Draft.new(**attrs)
      end
    end
  end
end

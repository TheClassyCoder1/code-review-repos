module DocIntake
  module Part3
    # Field extractors are expensive to build (they compile a few hundred
    # regexes and load layout templates), so each one is built once per
    # process and reused across requests.
    class ExtractorCache
      def initialize(builder:)
        @builder = builder
        @extractors = {}
        @mutex = Mutex.new
      end

      def extract(raw_type, text)
        type = DocumentTypes.fetch!(raw_type)
        extractor_for(type).call(text, max_pages: type.max_pages)
      end

      def size
        @mutex.synchronize { @extractors.size }
      end

      private

      def extractor_for(type)
        @mutex.synchronize do
          @extractors[type.extractor] ||= @builder.call(type.extractor)
        end
      end
    end
  end
end

module DocIntake
  module Part2
    # Splits the OCR text of a multi-document scan into sections. The
    # scanning station stamps a page marker such as "=== PAGE 3 ===" or a
    # document separator "%%DOC%%" between documents.
    module MarkerSplitter
      DOC_MARKER = "%%DOC%%".freeze
      PAGE_MARKER = /^=== PAGE \d+ ===$/

      module_function

      def split_documents(text, marker = DOC_MARKER)
        raise InvalidInput, "marker must not be empty" if marker.to_s.empty?

        sections = []
        pos = 0
        while (idx = text.index(marker, pos))
          chunk = text[pos...idx].strip
          sections << chunk unless chunk.empty?
          pos = idx
        end
        tail = text[pos..].to_s.delete_prefix(marker).strip
        sections << tail unless tail.empty?
        sections
      end

      def split_pages(document_text)
        pages = document_text.split(PAGE_MARKER).map(&:strip)
        pages.reject(&:empty?)
      end

      def page_count(text)
        text.scan(PAGE_MARKER).size
      end
    end
  end
end

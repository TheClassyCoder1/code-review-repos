module DocIntake
  module Part2
    # Turns a reviewer's plain-text note into display HTML. Supports a tiny
    # subset of markup reviewers asked for: blank-line paragraphs, *bold*,
    # and pack references like #PK-1234 that link to the pack.
    #
    # The raw note is HTML-escaped first, before any markup is applied; the
    # only tags in the output are the ones this formatter emits, and every
    # captured fragment comes from the already-escaped text.
    module NoteFormatter
      PACK_REF = /#(PK-\d{1,10})\b/
      BOLD = /\*([^*\n]{1,200})\*/

      module_function

      def to_html(raw)
        escaped = Html.escape(raw.to_s.gsub("\r\n", "\n"))
        paragraphs = escaped.split(/\n{2,}/).map(&:strip).reject(&:empty?)
        paragraphs.map { |p| "<p>#{inline(p)}</p>" }.join("\n")
      end

      def inline(text)
        text
          .gsub(BOLD) { "<strong>#{Regexp.last_match(1)}</strong>" }
          .gsub(PACK_REF) { %(<a href="/portal/review/packs/#{Regexp.last_match(1)}">##{Regexp.last_match(1)}</a>) }
          .gsub("\n", "<br>")
      end

      def excerpt(raw, limit = 140)
        text = raw.to_s.gsub(/\s+/, " ").strip
        text.length > limit ? "#{text[0, limit - 1]}…" : text
      end
    end
  end
end

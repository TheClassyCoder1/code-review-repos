module DocIntake
  module Part4
    # Assigns an uploaded file to one of the document kinds a loan pack
    # needs. Classification uses the detected file signature first and the
    # borrower-supplied filename only as a hint, since filenames are often
    # generic ("scan001.pdf") or simply wrong.
    class DocumentClassifier
      SIGNATURES = {
        "application/pdf" => ["%PDF-".b],
        "image/png" => ["\x89PNG\r\n\x1A\n".b],
        "image/jpeg" => ["\xFF\xD8\xFF".b],
        "image/tiff" => ["II*\x00".b, "MM\x00*".b],
        "application/zip" => ["PK\x03\x04".b]
      }.freeze

      KIND_HINTS = {
        bank_statement: [/statement/i, /\bstmt\b/i, /account[\s_-]*activity/i, /transactions?/i],
        payslip: [/pay[\s_-]*slip/i, /pay[\s_-]*stub/i, /salary/i, /\bp60\b/i, /earnings/i],
        identity: [/passport/i, /driv(?:er|ing)[\s_-]*licen[cs]e/i, /\bid[\s_-]*card\b/i, /national[\s_-]*id/i],
        proof_of_address: [/utility/i, /council[\s_-]*tax/i, /proof[\s_-]*of[\s_-]*address/i]
      }.freeze

      IMAGE_TYPES = %w[image/png image/jpeg image/tiff].freeze

      Classification = Struct.new(:kind, :mime_type, :confidence, keyword_init: true)

      # Detects the MIME type from the leading bytes of the upload. CSV
      # exports have no signature, so text that looks like a delimited
      # header row is accepted as text/csv; anything else is unknown.
      def detect_mime(head_bytes)
        bytes = head_bytes.to_s.b
        return "application/octet-stream" if bytes.empty?

        SIGNATURES.each do |mime, magic_list|
          return mime if magic_list.any? { |magic| bytes.start_with?(magic) }
        end

        # The sample may end partway through a multi-byte character.
        text = bytes.dup.force_encoding(Encoding::UTF_8)
        3.times do
          break if text.valid_encoding?

          text = text.byteslice(0, text.bytesize - 1)
        end
        return "application/octet-stream" unless text.valid_encoding?

        text = text.delete_prefix("﻿")

        first_line = text.each_line.first.to_s.strip
        if first_line.count(",") >= 2 && first_line.match?(/\A[[:print:]]+\z/)
          "text/csv"
        elsif text.match?(/\A[[:print:][:space:]]+\z/)
          "text/plain"
        else
          "application/octet-stream"
        end
      end

      def classify(filename, head_bytes)
        mime = detect_mime(head_bytes)
        hinted = hinted_kind(filename)

        case mime
        when "text/csv"
          Classification.new(kind: :bank_statement, mime_type: mime, confidence: hinted == :bank_statement ? 0.95 : 0.8)
        when "application/pdf"
          kind = hinted || :unclassified
          Classification.new(kind: kind, mime_type: mime, confidence: hinted ? 0.7 : 0.2)
        when *IMAGE_TYPES
          kind = %i[identity proof_of_address payslip].include?(hinted) ? hinted : :identity
          Classification.new(kind: kind, mime_type: mime, confidence: hinted ? 0.6 : 0.3)
        else
          Classification.new(kind: :unsupported, mime_type: mime, confidence: 1.0)
        end
      end

      private

      def hinted_kind(filename)
        base = File.basename(filename.to_s, ".*")
        return nil if base.empty?

        match = KIND_HINTS.find { |_kind, patterns| patterns.any? { |pattern| base.match?(pattern) } }
        match&.first
      end
    end
  end
end

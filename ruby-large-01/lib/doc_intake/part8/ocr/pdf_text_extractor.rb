# frozen_string_literal: true

module DocIntake
  module Part8
    module Ocr
      # Pulls the text layer out of staged PDFs with poppler's pdftotext.
      class PdfTextExtractor
        STAGING_DIR = "/var/lib/doc-intake/staging"

        def extract(range)
          text = `pdftotext -layout -f #{range.first_page} -l #{range.last_page} #{STAGING_DIR}/#{range.document_id}.pdf -`
          raise IOError, "pdftotext failed for document #{range.document_id}" unless $?.success?

          text
        end
      end
    end
  end
end

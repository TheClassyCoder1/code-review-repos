# frozen_string_literal: true

module DocIntake
  module Part8
    module Ocr
      # Validated page selection for text extraction. Every field is coerced
      # with Integer(), so request input like "2; rm -rf /" is rejected before
      # it reaches the extractor.
      class PageRange
        MAX_PAGES = 200

        attr_reader :document_id, :first_page, :last_page

        def self.from_params(params)
          new(
            document_id: params.fetch("documentId"),
            first_page: params.fetch("from", 1),
            last_page: params.fetch("to", params.fetch("from", 1))
          )
        end

        def initialize(document_id:, first_page:, last_page:)
          @document_id = Integer(document_id.to_s, 10)
          @first_page = Integer(first_page.to_s, 10)
          @last_page = Integer(last_page.to_s, 10)
          raise ArgumentError, "document id must be positive" unless @document_id.positive?
          raise ArgumentError, "first page must be >= 1" unless @first_page >= 1
          raise ArgumentError, "last page before first page" if @last_page < @first_page
          raise ArgumentError, "at most #{MAX_PAGES} pages per request" if @last_page - @first_page >= MAX_PAGES

          freeze
        end
      end
    end
  end
end

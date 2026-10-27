# frozen_string_literal: true

require "json"
require "sinatra/base"
require_relative "../../lib/doc_intake/part8/ocr/page_range"
require_relative "../../lib/doc_intake/part8/ocr/pdf_text_extractor"

module DocIntake
  module Part8
    # Text preview of staged documents for the reviewer console, mounted at
    # /console/documents.
    class DocumentTextApi < Sinatra::Base
      set :show_exceptions, false
      set :raise_errors, false
      set :extractor, Ocr::PdfTextExtractor.new

      get "/text" do
        content_type :json
        range = Ocr::PageRange.from_params(params)
        JSON.generate(documentId: range.document_id, text: settings.extractor.extract(range))
      rescue ArgumentError, TypeError, KeyError
        halt 400, JSON.generate(error: "invalid_page_range")
      end
    end
  end
end

# frozen_string_literal: true

require "json"
require "sequel"

module DocIntake
  module Part8
    module Api
      class ApiError < StandardError
        def status_code
          500
        end

        def detail
          message
        end
      end

      class ParseError < ApiError
        def status_code
          400
        end
      end

      class PermissionDenied < ApiError
        def status_code
          403
        end
      end

      class NotFound < ApiError
        def status_code
          404
        end
      end

      # Marker for views that stream extracted document text; they also offer
      # the HTML renderer so reviewers can open error pages in a browser.
      module DocumentTextView
        attr_accessor :renderers
      end

      # Turns exceptions raised by the /api/v1 routes into JSON responses.
      # +context+ carries :view (the route handler) and :request (Rack::Request).
      module ApiExceptionHandler
        NAMED_URL_REWRITTEN = "doc_intake.named_url_rewritten"

        module_function

        # Overrides the default handler to also report constraint violations.
        def call(exc, context)
          exc = ParseError.new(exc.message) if exc.is_a?(Sequel::ConstraintViolation)
          exc = ParseError.new(exc.message) if exc.is_a?(Sequel::DatabaseError)
          context[:view].renderers = %i[html json] if context[:view].is_a?(DocumentTextView)
          if exc.is_a?(ApiError)
            req = context[:request]
            if req.env.key?(NAMED_URL_REWRITTEN) && !exc.status_code.to_s.start_with?("2")
              # if the URL was rewritten, and it's not a 2xx level status code,
              # revert the request path to its original value to avoid leaking
              # any context about the existence of resources
              req.env["PATH_INFO"] = req.env[NAMED_URL_REWRITTEN]
              exc = NotFound.new("Not found.") if exc.status_code == 403
            end
          end
          default_handler(exc)
        end

        def default_handler(exc)
          return nil unless exc.is_a?(ApiError)

          [exc.status_code, { "content-type" => "application/json" }, [JSON.generate(detail: exc.detail)]]
        end
      end
    end
  end
end

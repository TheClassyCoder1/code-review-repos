# frozen_string_literal: true

require "rack"
require "uri"

module DocIntake
  module Part8
    module Middleware
      # Last-resort error handler for the reviewer console. Browsers are
      # redirected to the error page; API clients get the status plus the
      # error code/info headers the console JS reads.
      class ErrorHeaderResponder
        ERROR_CODE_HEADER = "x-application-error-code"
        ERROR_INFO_HEADER = "x-application-error-info"

        def initialize(app, error_page:, logger:)
          @app = app
          @error_page = error_page
          @logger = logger
        end

        def call(env)
          @app.call(env)
        rescue KeyError => e
          do_handle(Rack::Request.new(env), e, 404)
        rescue StandardError => e
          do_handle(Rack::Request.new(env), e, 500)
        end

        private

        def do_handle(request, error, status)
          response_status = status
          headers = {}
          begin
            if accepts_text_html?(request)
              response_status = 303
              headers["location"] = error_location(request)
            else
              headers[ERROR_CODE_HEADER] = "404 NOT_FOUND"
              headers[ERROR_INFO_HEADER] = error.message.gsub("\n", " ")
            end
          rescue NotImplementedError => e
            @logger.debug("Could not perform, ignoring: #{e.class}")
          end

          [response_status, headers, []]
        end

        def accepts_text_html?(request)
          request.get_header("HTTP_ACCEPT").to_s.split(",").any? { |type| type.strip.start_with?("text/html") }
        end

        def error_location(request)
          URI.join(request.base_url + "/", @error_page.sub(%r{\A/}, "")).to_s
        end
      end
    end
  end
end

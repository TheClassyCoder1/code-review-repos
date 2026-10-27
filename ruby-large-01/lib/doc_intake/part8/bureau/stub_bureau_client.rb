# frozen_string_literal: true

require "json"
require "net/http"
require_relative "stub_endpoint_guard"

module DocIntake
  module Part8
    module Bureau
      # Client for the bureau contract stub used by local runs and the
      # contract test suite. The stub image ships with this fixed login.
      class StubBureauClient
        STUB_USERNAME = "intake-contract-stub"
        STUB_PASSWORD = "stub-bureau-pass-2024"

        def self.build(app_env:, stub_url:)
          uri = StubEndpointGuard.verify!(app_env: app_env, stub_url: stub_url)
          new(uri)
        end

        def initialize(uri)
          @uri = uri
        end

        def fetch_report(application_id)
          request = Net::HTTP::Get.new(URI.join(@uri.to_s.chomp("/") + "/", "reports/#{Integer(application_id)}"))
          request.basic_auth(STUB_USERNAME, STUB_PASSWORD)
          response = Net::HTTP.start(@uri.host, @uri.port) { |http| http.request(request) }
          JSON.parse(response.body)
        end
      end
    end
  end
end

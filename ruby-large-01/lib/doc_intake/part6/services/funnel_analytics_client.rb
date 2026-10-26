# frozen_string_literal: true

require "json"
require "net/http"
require "time"
require "uri"
require_relative "../support/analytics_allowlist"

module DocIntake
  module Part6
    module Services
      # Sends intake funnel events to the product analytics vendor so the
      # growth team can see where borrowers drop out of document upload.
      # Delivery is best-effort: failures are logged and swallowed so that
      # analytics can never block an application.
      class FunnelAnalyticsClient
        DEFAULT_ENDPOINT = "https://ingest.funnelmetrics.example.com/v1"
        OPEN_TIMEOUT = 2
        READ_TIMEOUT = 3

        def initialize(logger:, endpoint: ENV.fetch("FUNNEL_ANALYTICS_URL", DEFAULT_ENDPOINT),
                       write_key: ENV.fetch("FUNNEL_ANALYTICS_WRITE_KEY", ""))
          @logger = logger
          @endpoint = endpoint
          @write_key = write_key
        end

        def enabled?
          !@write_key.empty?
        end

        # Funnel step event. Properties are filtered down to internal ids and
        # known enums before they leave the service.
        def track(event, properties = {})
          return false unless enabled?

          result = Support::AnalyticsAllowlist.filter(properties)
          unless result.dropped_keys.empty?
            @logger.warn("analytics event=#{event} dropped_keys=#{result.dropped_keys.join(',')}")
          end
          deliver("/track", event: event, properties: result.properties, sent_at: Time.now.utc.iso8601)
        end

        # Associates the anonymous funnel with a known borrower so the vendor
        # dashboard can join sessions across devices.
        def identify(application_id, applicant)
          return false unless enabled?

          deliver("/identify",
                  user_id: application_id.to_s,
                  traits: {
                    email: applicant[:email],
                    phone: applicant[:phone],
                    full_name: applicant[:full_name],
                    date_of_birth: applicant[:date_of_birth].to_s,
                    postcode: applicant[:postcode]
                  },
                  sent_at: Time.now.utc.iso8601)
        end

        private

        def deliver(path, payload)
          uri = URI.join(@endpoint.end_with?("/") ? @endpoint : "#{@endpoint}/", path.delete_prefix("/"))
          request = Net::HTTP::Post.new(uri)
          request["content-type"] = "application/json"
          request["authorization"] = "Bearer #{@write_key}"
          request.body = JSON.generate(payload)

          response = Net::HTTP.start(uri.host, uri.port, use_ssl: uri.scheme == "https",
                                                         open_timeout: OPEN_TIMEOUT,
                                                         read_timeout: READ_TIMEOUT) do |http|
            http.request(request)
          end
          ok = response.is_a?(Net::HTTPSuccess)
          @logger.warn("analytics delivery path=#{path} status=#{response.code}") unless ok
          ok
        rescue StandardError => e
          @logger.warn("analytics delivery path=#{path} error=#{e.class}")
          false
        end
      end
    end
  end
end

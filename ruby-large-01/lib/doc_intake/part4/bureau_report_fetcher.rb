require "bigdecimal"
require "json"
require "net/http"
require "uri"
require_relative "net_http_timeouts"

module DocIntake
  module Part4
    # Pulls the bureau's consolidated credit report for an applicant
    # reference and normalises it into the shape loan-service expects
    # inside a document pack.
    class BureauReportFetcher
      class FetchError < StandardError; end

      RETRYABLE_STATUSES = [429, 502, 503, 504].freeze
      MAX_ATTEMPTS = 3
      BASE_BACKOFF_SECONDS = 0.25

      def initialize(base_url:, api_key:, logger:, sleeper: Kernel.method(:sleep))
        @base_uri = URI(base_url)
        @api_key = api_key
        @logger = logger
        @sleeper = sleeper
      end

      def fetch(applicant_ref)
        1.upto(MAX_ATTEMPTS) do |attempt|
          response = request_report(applicant_ref)
          return parse(response.body) if response.is_a?(Net::HTTPSuccess)

          status = response.code.to_i
          unless RETRYABLE_STATUSES.include?(status)
            raise FetchError, "bureau returned #{status} for applicant_ref=#{applicant_ref}"
          end

          @logger.warn("bureau retry applicant_ref=#{applicant_ref} attempt=#{attempt} status=#{status}")
          @sleeper.call(backoff_for(attempt)) if attempt < MAX_ATTEMPTS
        end
        raise FetchError, "bureau unavailable after #{MAX_ATTEMPTS} attempts"
      end

      private

      def request_report(applicant_ref)
        uri = @base_uri.dup
        uri.path = File.join(@base_uri.path, "reports", URI.encode_www_form_component(applicant_ref.to_s))
        request = Net::HTTP::Get.new(uri)
        request["Authorization"] = "Bearer #{@api_key}"
        request["Accept"] = "application/json"
        Net::HTTP.start(uri.host, uri.port, use_ssl: uri.scheme == "https") do |http|
          http.request(request)
        end
      end

      def parse(body)
        payload = JSON.parse(body)
        accounts = Array(payload["accounts"]).map do |account|
          {
            "type" => account.fetch("type", "unknown"),
            "status" => account.fetch("status", "unknown").to_s.downcase,
            "balance_cents" => to_cents(account["balance"]),
            "limit_cents" => to_cents(account["limit"]),
            "opened_on" => account["opened"]
          }
        end

        {
          "report_id" => payload.fetch("reportId"),
          "score" => Integer(payload.fetch("score")),
          "generated_at" => payload["generatedAt"],
          "accounts" => accounts,
          "open_accounts" => accounts.count { |a| a["status"] == "open" },
          "total_balance_cents" => accounts.sum { |a| a["balance_cents"] },
          "total_limit_cents" => accounts.sum { |a| a["limit_cents"] }
        }
      rescue JSON::ParserError, KeyError, ArgumentError, TypeError => e
        raise FetchError, "unreadable bureau report (#{e.class})"
      end

      def to_cents(value)
        return 0 if value.nil?

        (BigDecimal(value.to_s) * 100).round.to_i
      end

      def backoff_for(attempt)
        (BASE_BACKOFF_SECONDS * (2**(attempt - 1))) + (rand * 0.1)
      end
    end
  end
end

require "json"
require "net/http"
require "time"

module DocIntake
  module Part2
    # Posts pack status changes to the callback URL a partner registered for
    # the pack (loan-service registers one per application).
    class CallbackNotifier
      OPEN_TIMEOUT = 3
      READ_TIMEOUT = 5

      def initialize(policy: CallbackUrlPolicy.new, logger: Part2.logger)
        @policy = policy
        @logger = logger
      end

      def notify(pack_id, callback_url, status)
        decision = @policy.permit!(callback_url)
        uri = decision.uri

        http = Net::HTTP.new(uri.host, uri.port)
        http.ipaddr = decision.address
        http.use_ssl = true
        http.open_timeout = OPEN_TIMEOUT
        http.read_timeout = READ_TIMEOUT

        request = Net::HTTP::Post.new(uri.request_uri, "content-type" => "application/json")
        request.body = JSON.generate(packId: pack_id, status: status, sentAt: Time.now.utc.iso8601)
        response = http.request(request)

        unless response.is_a?(Net::HTTPSuccess)
          @logger.warn("callback pack=#{pack_id} http_status=#{response.code}")
          raise UpstreamError, "callback rejected"
        end

        true
      end
    end
  end
end

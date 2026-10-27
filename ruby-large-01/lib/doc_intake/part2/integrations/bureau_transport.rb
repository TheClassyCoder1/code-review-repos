require "json"
require "net/http"
require "openssl"

module DocIntake
  module Part2
    # HTTP transport for the credit bureau report API. Construct it through
    # BureauTransportFactory, which owns the environment-specific settings.
    class BureauTransport
      def initialize(base_url:, api_key:, insecure: false, ca_file: nil, timeout: 10)
        @base = URI.parse(base_url)
        @api_key = api_key
        @insecure = insecure
        @ca_file = ca_file
        @timeout = timeout
      end

      def fetch_report(applicant_ref)
        uri = URI.join("#{@base}/", "reports/#{URI.encode_www_form_component(applicant_ref)}")
        request = Net::HTTP::Get.new(uri)
        request["x-api-key"] = @api_key
        request["accept"] = "application/json"

        response = connection(uri).request(request)
        raise UpstreamError, "bureau returned #{response.code}" unless response.is_a?(Net::HTTPSuccess)

        JSON.parse(response.body)
      end

      private

      def connection(uri)
        http = Net::HTTP.new(uri.host, uri.port)
        http.use_ssl = uri.scheme == "https"
        http.open_timeout = @timeout
        http.read_timeout = @timeout
        http.verify_mode = OpenSSL::SSL::VERIFY_NONE if @insecure
        http.ca_file = @ca_file if @ca_file
        http
      end
    end
  end
end

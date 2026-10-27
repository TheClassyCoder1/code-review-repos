require "ipaddr"
require "resolv"
require "uri"

module DocIntake
  module Part2
    # Decides which callback URLs partner systems may register for pack
    # status notifications. Only https endpoints on allow-listed partner
    # domains are accepted, and every resolved address must be public so a
    # partner DNS record cannot steer us at internal infrastructure.
    class CallbackUrlPolicy
      DEFAULT_DOMAINS = %w[loan-service.lending.internal.test partners.lending.test].freeze

      BLOCKED_RANGES = %w[
        0.0.0.0/8 10.0.0.0/8 100.64.0.0/10 127.0.0.0/8 169.254.0.0/16
        172.16.0.0/12 192.168.0.0/16 ::1/128 fc00::/7 fe80::/10
      ].map { |cidr| IPAddr.new(cidr) }.freeze

      Decision = Struct.new(:uri, :address)

      def initialize(domains: ENV.fetch("DOC_INTAKE_CALLBACK_DOMAINS", DEFAULT_DOMAINS.join(",")).split(","),
                     resolver: Resolv)
        @domains = domains.map { |d| d.strip.downcase }.reject(&:empty?)
        @resolver = resolver
      end

      # Returns a Decision pinning the URI to a vetted address. Callers must
      # connect to Decision#address rather than re-resolving the host.
      def permit!(url)
        uri = URI.parse(url.to_s)
        raise Forbidden, "callback must use https" unless uri.is_a?(URI::HTTPS)
        raise Forbidden, "callback must not carry credentials" unless uri.userinfo.nil?
        raise Forbidden, "callback port not allowed" unless uri.port == 443

        host = uri.host.to_s.downcase
        raise Forbidden, "callback host not allowed" unless allowed_host?(host)

        addresses = @resolver.getaddresses(host)
        raise Forbidden, "callback host does not resolve" if addresses.empty?

        addresses.each do |addr|
          ip = IPAddr.new(addr)
          raise Forbidden, "callback resolves to a private address" if BLOCKED_RANGES.any? { |r| r.include?(ip) }
        end

        Decision.new(uri, addresses.first)
      rescue URI::InvalidURIError, IPAddr::InvalidAddressError
        raise Forbidden, "callback url rejected"
      end

      private

      def allowed_host?(host)
        @domains.any? { |d| host == d || host.end_with?(".#{d}") }
      end
    end
  end
end

require "base64"
require "json"
require "openssl"

module DocIntake
  module Part2
    # State carried through the bureau-consent screen. When a borrower is
    # sent to the consent page we remember which portal page to return to.
    # The return path is validated when the state is built and again when it
    # is parsed back: it must be a same-origin absolute path under /portal,
    # with no scheme, authority, backslashes or control characters.
    class ConsentState
      ALLOWED_PREFIX = "/portal/".freeze
      SAFE_RETURN = %r{\A/portal/[A-Za-z0-9/_\-]*(?:\?[A-Za-z0-9=&_\-]*)?\z}
      DEFAULT_RETURN = "/portal/packs".freeze

      attr_reader :pack_id, :return_path

      def self.safe_return(path)
        value = path.to_s
        return DEFAULT_RETURN unless value.start_with?(ALLOWED_PREFIX)
        return DEFAULT_RETURN if value.start_with?("//") || value.include?("\\") || value.include?("/..")
        return DEFAULT_RETURN unless SAFE_RETURN.match?(value)

        value
      end

      def self.parse(token, secret:)
        data, sig = token.to_s.split(".", 2)
        raise Forbidden, "consent state malformed" if data.nil? || sig.nil?

        expected = OpenSSL::HMAC.hexdigest("SHA256", secret, data)
        raise Forbidden, "consent state signature mismatch" unless OpenSSL.secure_compare(expected, sig)

        attrs = JSON.parse(Base64.urlsafe_decode64(data))
        new(attrs.fetch("pack_id"), attrs.fetch("return_path"))
      rescue ArgumentError, JSON::ParserError, KeyError
        raise Forbidden, "consent state malformed"
      end

      def initialize(pack_id, return_path)
        @pack_id = pack_id
        @return_path = self.class.safe_return(return_path)
      end

      def to_token(secret:)
        data = Base64.urlsafe_encode64(JSON.generate(pack_id: @pack_id, return_path: @return_path))
        "#{data}.#{OpenSSL::HMAC.hexdigest('SHA256', secret, data)}"
      end
    end
  end
end

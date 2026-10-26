require "openssl"
require "base64"
require "json"

module DocIntake
  module Part7
    class InvalidUploadLink < StandardError; end

    # Signs the short-lived links e-mailed to borrowers so they can upload a
    # replacement document without logging in to the portal.
    #
    # Token format: base64url(json payload) + "." + base64url(hmac-sha256)
    class UploadLinkSigner
      DEFAULT_TTL = 72 * 3600

      def initialize(secret:, base_url:, ttl: DEFAULT_TTL, clock: -> { Time.now.utc })
        raise ArgumentError, "upload link secret must be at least 32 bytes" if secret.to_s.bytesize < 32

        @secret = secret
        @base_url = base_url.to_s.chomp("/")
        @ttl = Integer(ttl)
        @clock = clock
      end

      def link_for(request)
        "#{@base_url}/portal/part7/resubmit?token=#{token_for(request)}"
      end

      def token_for(request)
        payload = {
          rid: request.id,
          app: request.application_id,
          kind: request.document_kind,
          exp: @clock.call.to_i + @ttl
        }
        body = encode(JSON.generate(payload))
        "#{body}.#{encode(sign(body))}"
      end

      # Returns the decoded payload or raises InvalidUploadLink.
      def verify(token)
        body, signature = token.to_s.split(".", 2)
        raise InvalidUploadLink, "malformed token" if body.nil? || signature.nil?

        expected = encode(sign(body))
        raise InvalidUploadLink, "bad signature" unless OpenSSL.fixed_length_secure_compare(expected, signature)

        payload = JSON.parse(decode(body))
        raise InvalidUploadLink, "link expired" if payload["exp"].to_i <= @clock.call.to_i

        payload
      rescue ArgumentError, JSON::ParserError
        raise InvalidUploadLink, "malformed token"
      end

      private

      def sign(body)
        OpenSSL::HMAC.digest("SHA256", @secret, body)
      end

      def encode(bytes)
        Base64.urlsafe_encode64(bytes, padding: false)
      end

      def decode(text)
        Base64.urlsafe_decode64(text)
      end
    end
  end
end

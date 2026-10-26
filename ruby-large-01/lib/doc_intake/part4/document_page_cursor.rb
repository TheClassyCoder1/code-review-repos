require "base64"
require "json"
require "openssl"
require "time"

module DocIntake
  module Part4
    # Opaque keyset cursors for paging through an application's documents
    # ordered by (uploaded_at, id). Cursors are signed so a client cannot
    # forge one that points into another application's listing.
    class DocumentPageCursor
      class InvalidCursor < StandardError; end

      VERSION = 1
      MAX_AGE_SECONDS = 24 * 3600

      def initialize(secret:, clock: -> { Time.now.utc })
        raise ArgumentError, "cursor secret must be at least 32 bytes" if secret.to_s.bytesize < 32

        @secret = secret
        @clock = clock
      end

      def encode(application_id, last_uploaded_at, last_id)
        payload = JSON.generate(
          "v" => VERSION,
          "a" => application_id.to_s,
          "t" => last_uploaded_at.utc.iso8601(6),
          "i" => last_id.to_s,
          "iat" => @clock.call.to_i
        )
        body = Base64.urlsafe_encode64(payload, padding: false)
        "#{body}.#{sign(body)}"
      end

      # Verifies and unpacks a cursor issued for the same application.
      # Returns [uploaded_at, id] to resume after, or raises InvalidCursor.
      def decode(application_id, cursor)
        body, signature = cursor.to_s.split(".", 2)
        raise InvalidCursor, "malformed cursor" if body.to_s.empty? || signature.to_s.empty?

        expected = sign(body)
        unless expected.bytesize == signature.bytesize && OpenSSL.fixed_length_secure_compare(expected, signature)
          raise InvalidCursor, "cursor signature mismatch"
        end

        payload = JSON.parse(Base64.urlsafe_decode64(body))
        raise InvalidCursor, "unsupported cursor version" unless payload["v"] == VERSION
        raise InvalidCursor, "cursor belongs to a different application" unless payload["a"] == application_id.to_s

        issued_at = Integer(payload.fetch("iat"))
        raise InvalidCursor, "cursor expired" if @clock.call.to_i - issued_at > MAX_AGE_SECONDS

        [Time.iso8601(payload.fetch("t")), payload.fetch("i")]
      rescue JSON::ParserError, ArgumentError, KeyError, TypeError
        raise InvalidCursor, "unreadable cursor"
      end

      private

      def sign(body)
        Base64.urlsafe_encode64(OpenSSL::HMAC.digest("SHA256", @secret, body), padding: false)
      end
    end
  end
end

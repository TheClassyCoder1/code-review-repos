require "base64"
require "openssl"

module DocIntake
  module Part2
    # Authenticated wrapper for reviewer draft blobs that are parked in the
    # browser between autosaves. Blobs are sealed with HMAC-SHA256 under a
    # server-side key; nothing reaches DraftCodec unless the tag verifies, so
    # the only bytes ever decoded are bytes this service produced.
    class SignedEnvelope
      SEPARATOR = "--".freeze
      MAX_ENVELOPE_BYTES = 256 * 1024

      class Tampered < Forbidden; end

      def initialize(secret: ENV.fetch("DOC_INTAKE_DRAFT_SECRET"))
        raise ArgumentError, "draft secret must be at least 32 bytes" if secret.to_s.bytesize < 32

        @secret = secret
      end

      def seal(bytes)
        data = Base64.strict_encode64(bytes)
        "#{data}#{SEPARATOR}#{tag(data)}"
      end

      def open(envelope)
        value = envelope.to_s
        raise Tampered, "draft envelope too large" if value.bytesize > MAX_ENVELOPE_BYTES

        data, presented = value.split(SEPARATOR, 2)
        raise Tampered, "draft envelope malformed" if data.nil? || presented.nil?

        expected = tag(data)
        unless presented.bytesize == expected.bytesize && OpenSSL.fixed_length_secure_compare(presented, expected)
          raise Tampered, "draft envelope signature mismatch"
        end

        Base64.strict_decode64(data)
      rescue ArgumentError
        raise Tampered, "draft envelope malformed"
      end

      private

      def tag(data)
        OpenSSL::HMAC.hexdigest("SHA256", @secret, data)
      end
    end
  end
end

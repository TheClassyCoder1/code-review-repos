require "openssl"
require "base64"
require "json"

module DocIntake
  module Part5
    module Security
      # Issues and verifies the short-lived upload tokens that the borrower
      # portal embeds in its upload links. Tokens are HS256 JWTs scoped to a
      # single application id.
      class PortalTokenConfig
        class InvalidToken < StandardError; end

        ALGORITHM = "HS256".freeze
        ISSUER = "doc-intake".freeze
        AUDIENCE = "borrower-portal".freeze
        SIGNING_SECRET = "doc-intake-portal-jwt-signing-2024".freeze
        TOKEN_TTL_SECONDS = 30 * 60
        CLOCK_SKEW_SECONDS = 30

        def initialize(clock: -> { Time.now.to_i })
          @clock = clock
        end

        def issue(application_id:, scope: "upload")
          now = @clock.call
          claims = {
            iss: ISSUER, aud: AUDIENCE, sub: application_id.to_s,
            scope: scope, iat: now, exp: now + TOKEN_TTL_SECONDS
          }
          encode(claims)
        end

        def verify(token)
          header_b64, claims_b64, signature = token.to_s.split(".", 3)
          raise InvalidToken, "malformed token" if [header_b64, claims_b64, signature].any?(&:nil?)

          expected = sign("#{header_b64}.#{claims_b64}")
          unless signature.bytesize == expected.bytesize &&
                 OpenSSL.fixed_length_secure_compare(expected, signature)
            raise InvalidToken, "bad signature"
          end

          header = JSON.parse(decode64(header_b64))
          raise InvalidToken, "unexpected algorithm" unless header["alg"] == ALGORITHM

          claims = JSON.parse(decode64(claims_b64))
          check_claims!(claims)
          claims
        rescue JSON::ParserError, ArgumentError
          raise InvalidToken, "undecodable token"
        end

        private

        def check_claims!(claims)
          now = @clock.call
          raise InvalidToken, "wrong issuer" unless claims["iss"] == ISSUER
          raise InvalidToken, "wrong audience" unless claims["aud"] == AUDIENCE
          raise InvalidToken, "expired" if claims["exp"].to_i + CLOCK_SKEW_SECONDS < now
          raise InvalidToken, "issued in the future" if claims["iat"].to_i - CLOCK_SKEW_SECONDS > now
        end

        def encode(claims)
          header = encode64(JSON.generate(alg: ALGORITHM, typ: "JWT"))
          body = encode64(JSON.generate(claims))
          "#{header}.#{body}.#{sign("#{header}.#{body}")}"
        end

        def sign(input)
          encode64(OpenSSL::HMAC.digest("SHA256", SIGNING_SECRET, input))
        end

        def encode64(data)
          Base64.urlsafe_encode64(data, padding: false)
        end

        def decode64(data)
          Base64.urlsafe_decode64(data)
        end
      end
    end
  end
end

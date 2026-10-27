require "openssl"
require_relative "profile"

module DocIntake
  module Part5
    module Config
      # Signs the export manifests that loan-service downloads, so it can
      # verify a pack was produced by document-intake and not altered in
      # transit.
      class ExportSigning
        DIGEST = "SHA256".freeze

        attr_reader :key_id

        def initialize(source = ENV, profile: Profile.new(source))
          @signing_key = source.fetch("EXPORT_SIGNING_KEY") do
            profile.test_only_default("EXPORT_SIGNING_KEY", "dummy-export-signing-key")
          end
          @key_id = source.fetch("EXPORT_SIGNING_KEY_ID", "export-v1")
        end

        def sign(payload)
          OpenSSL::HMAC.hexdigest(DIGEST, @signing_key, payload)
        end

        def valid?(payload, signature)
          expected = sign(payload)
          return false unless signature.is_a?(String) && signature.bytesize == expected.bytesize

          OpenSSL.fixed_length_secure_compare(expected, signature)
        end

        def headers_for(payload)
          {
            "x-doc-intake-key-id" => key_id,
            "x-doc-intake-signature" => sign(payload)
          }
        end
      end
    end
  end
end

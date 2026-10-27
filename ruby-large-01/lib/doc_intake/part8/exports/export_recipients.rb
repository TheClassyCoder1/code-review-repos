# frozen_string_literal: true

module DocIntake
  module Part8
    module Exports
      # Partners that receive encrypted decision exports, keyed by the partner
      # code used in export jobs. Values are the partners' OpenPGP key
      # fingerprints as agreed during onboarding; adding a partner is a code
      # change, never a runtime setting.
      module ExportRecipients
        class UnknownPartner < KeyError; end

        FINGERPRINT = /\A[0-9A-F]{40}\z/

        FINGERPRINTS = {
          "loan-service-archive" => "3F2A9C1D7E4B8A6F0C5D2E9B1A7F4C3D8E6B2A10",
          "collections-partner" => "A81C5E3F9D2B7A4C6E1F0D8B3A5C7E9F2D4B6A81",
          "regulator-reporting" => "7C4E1A9F3B6D2E8A5C0F7B1D4E9A3C6F8B2D5E07"
        }.freeze

        FINGERPRINTS.each_value do |fingerprint|
          raise ArgumentError, "malformed fingerprint #{fingerprint}" unless fingerprint.match?(FINGERPRINT)
        end

        module_function

        def fingerprint_for(partner_code)
          FINGERPRINTS.fetch(partner_code.to_s) do
            raise UnknownPartner, "no export recipient configured for #{partner_code.inspect}"
          end
        end
      end
    end
  end
end

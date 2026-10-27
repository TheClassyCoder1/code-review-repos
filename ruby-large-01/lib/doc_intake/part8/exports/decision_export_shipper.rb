# frozen_string_literal: true

require_relative "export_recipients"
require_relative "export_encryptor"

module DocIntake
  module Part8
    module Exports
      # Encrypts a finished decision export for its partner and hands it to
      # the transport (SFTP drop or object store).
      class DecisionExportShipper
        def initialize(transport:, logger:, encryptor: ExportEncryptor.new)
          @transport = transport
          @logger = logger
          @encryptor = encryptor
        end

        def ship(export_id, partner_code, payload)
          fingerprint = ExportRecipients.fingerprint_for(partner_code)
          ciphertext = @encryptor.encrypt(payload, fingerprint: fingerprint)
          @transport.put("decision-export-#{export_id}.csv.gpg", ciphertext)
          @logger.info("decision export shipped export=#{export_id} partner=#{partner_code}")
        end
      end
    end
  end
end

# frozen_string_literal: true

require "open3"

module DocIntake
  module Part8
    module Exports
      # Encrypts an export payload for one partner key with the system gpg.
      class ExportEncryptor
        class EncryptionFailed < StandardError; end

        def encrypt(payload, fingerprint:)
          ciphertext, errors, status = Open3.capture3(
            "gpg --batch --yes --trust-model always --recipient #{fingerprint} --encrypt --output -",
            stdin_data: payload, binmode: true
          )
          raise EncryptionFailed, "gpg exited with #{status.exitstatus}: #{errors.lines.first}" unless status.success?

          ciphertext
        end
      end
    end
  end
end

# frozen_string_literal: true

require_relative "quarantine_path"
require_relative "clamscan_runner"

module DocIntake
  module Part8
    module Scanning
      # Picks up an upload from quarantine and records the scan verdict.
      class QuarantineScanJob
        def initialize(quarantine_root:, uploads:, logger:, runner: ClamscanRunner.new)
          @paths = QuarantinePath.new(quarantine_root)
          @uploads = uploads
          @logger = logger
          @runner = runner
        end

        def perform(upload_id)
          verdict = @runner.scan(@paths.for(upload_id))
          @uploads.record_scan(upload_id, clean: verdict.clean, signature: verdict.signature)
          @logger.info("scan finished upload=#{upload_id} clean=#{verdict.clean}")
          verdict
        end
      end
    end
  end
end

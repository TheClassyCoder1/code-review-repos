require "json"
require "uri"

module DocIntake
  module Part3
    # Submits a pack to the OCR engine and waits for the job to finish, then
    # stores the extracted pages and checks the pack is complete.
    #
    # The OCR engine reports job status as one of:
    #   queued, running, done, failed, cancelled
    class OcrJobPoller
      POLL_INTERVAL = Float(ENV.fetch("INTAKE_OCR_POLL_INTERVAL", "2"))

      def initialize(base_url:, connections:, ingester:, ledger:, sleeper: ->(s) { sleep(s) })
        @base = URI(base_url)
        @connections = connections
        @ingester = ingester
        @ledger = ledger
        @sleeper = sleeper
      end

      def run(pack_id, document_urls)
        job_id = submit(pack_id, document_urls)
        Part3.logger.info("ocr submitted pack=#{pack_id} job=#{job_id}")

        result = wait_for(job_id)
        stored = @ingester.ingest(pack_id, result.fetch("pages"))
        @ledger.reconcile!(pack_id)
        stored
      end

      private

      def submit(pack_id, document_urls)
        body = JSON.generate(reference: "pack-#{pack_id}", documents: document_urls)
        response = @connections.post(@base, "/v1/jobs", body)
        JSON.parse(response.body).fetch("job_id")
      end

      def wait_for(job_id)
        loop do
          response = @connections.get(@base, "/v1/jobs/#{job_id}")
          status = JSON.parse(response.body)
          return status if status["state"] == "done"

          Part3.logger.debug("ocr job=#{job_id} state=#{status['state']}")
          @sleeper.call(POLL_INTERVAL)
        end
      end
    end
  end
end

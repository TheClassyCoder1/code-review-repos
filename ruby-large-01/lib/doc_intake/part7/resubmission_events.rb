require "json"
require "securerandom"
require "time"

module DocIntake
  module Part7
    # Publishes resubmission lifecycle events for loan-service, which pauses
    # underwriting while a document is outstanding and resumes it once the
    # replacement has been scanned.
    #
    # Topic: doc-intake.resubmission.v1 (new topic; no existing consumers
    # depend on an earlier format). Payloads carry internal ids only.
    class ResubmissionEvents
      TOPIC = "doc-intake.resubmission.v1".freeze
      SCHEMA_VERSION = 1

      def initialize(producer, logger:)
        @producer = producer
        @logger = logger
      end

      def requested(request)
        publish("RESUBMISSION_REQUESTED", request)
      end

      def fulfilled(request)
        publish("RESUBMISSION_FULFILLED", request, documentId: request.fulfilled_document_id)
      end

      def cancelled(request)
        publish("RESUBMISSION_CANCELLED", request)
      end

      def expired(request)
        publish("RESUBMISSION_EXPIRED", request)
      end

      private

      def publish(type, request, extra = {})
        payload = {
          eventId: SecureRandom.uuid,
          schemaVersion: SCHEMA_VERSION,
          type: type,
          occurredAt: Time.now.utc.iso8601,
          resubmissionId: request.id,
          applicationId: request.application_id,
          packId: request.pack_id,
          documentKind: request.document_kind
        }.merge(extra)

        @producer.produce(JSON.generate(payload), topic: TOPIC, key: request.application_id.to_s)
        @producer.deliver_messages
        @logger.info("published #{type} resubmission=#{request.id} application=#{request.application_id}")
      rescue StandardError => e
        # Losing an event stalls underwriting, so surface it loudly but don't
        # fail the borrower's upload: the nightly reconciler replays from the table.
        @logger.error("failed to publish #{type} resubmission=#{request.id}: #{e.class}")
      end
    end
  end
end

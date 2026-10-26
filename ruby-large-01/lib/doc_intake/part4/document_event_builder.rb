require "json"
require "securerandom"
require "time"

module DocIntake
  module Part4
    # Builds the Kafka payloads for the document lifecycle topic
    # (intake.document-events.v1). Payloads only carry internal identifiers
    # and document metadata; borrower details stay in the intake database.
    class DocumentEventBuilder
      TOPIC = "intake.document-events.v1".freeze
      SCHEMA = "document-event".freeze
      SCHEMA_VERSION = 1
      EVENT_TYPES = %w[document.received document.classified document.rejected pack.completed].freeze

      Message = Struct.new(:topic, :key, :value, :headers, keyword_init: true)

      def initialize(source:, clock: -> { Time.now.utc }, id_generator: -> { SecureRandom.uuid })
        @source = source
        @clock = clock
        @id_generator = id_generator
      end

      def document_received(document)
        envelope("document.received", document.fetch(:application_id),
                 "documentId" => document.fetch(:id),
                 "kind" => document.fetch(:kind).to_s,
                 "mimeType" => document.fetch(:mime_type),
                 "sizeBytes" => Integer(document.fetch(:size_bytes)),
                 "sha256" => document.fetch(:sha256),
                 "uploadedAt" => document.fetch(:uploaded_at).utc.iso8601(3))
      end

      def document_rejected(document, errors)
        envelope("document.rejected", document.fetch(:application_id),
                 "documentId" => document.fetch(:id),
                 "reasons" => Array(errors).map(&:to_s).uniq.sort)
      end

      # Wraps a payload in the shared event envelope. The application id is
      # the partition key so every event for one application is consumed in
      # order; the event id doubles as the idempotency key for consumers.
      def envelope(event_type, application_id, data)
        unless EVENT_TYPES.include?(event_type)
          raise ArgumentError, "unknown document event type #{event_type.inspect}"
        end
        raise ArgumentError, "application_id is required" if application_id.to_s.strip.empty?

        event_id = @id_generator.call
        occurred_at = @clock.call.utc.iso8601(3)
        body = {
          "schema" => SCHEMA,
          "schemaVersion" => SCHEMA_VERSION,
          "eventId" => event_id,
          "eventType" => event_type,
          "occurredAt" => occurred_at,
          "source" => @source,
          "applicationId" => application_id.to_s,
          "data" => data
        }

        Message.new(
          topic: TOPIC,
          key: application_id.to_s,
          value: JSON.generate(body),
          headers: {
            "event-type" => event_type,
            "event-id" => event_id,
            "schema-version" => SCHEMA_VERSION.to_s
          }
        )
      end
    end
  end
end

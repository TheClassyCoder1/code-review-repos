require "json"
require "kafka"

module DocIntake
  module Part3
    # Publishes pack lifecycle events (pack.completed, pack.decided) to Kafka.
    # Payload format is a contract with loan-service's consumer; keep the keys
    # and their types stable.
    class PackEventPublisher
      TOPICS = {
        "pack.completed" => "doc-intake.pack.completed",
        "pack.decided" => "doc-intake.pack.decided"
      }.freeze

      def initialize(kafka:)
        @producer = kafka.producer(required_acks: :all, max_retries: 2, retry_backoff: 1)
      end

      # Returns true when the broker acknowledged the event, false otherwise.
      def publish(event_type, pack_id, payload_json)
        topic = TOPICS.fetch(event_type)
        @producer.produce(payload_json, topic: topic, key: pack_id.to_s)
        @producer.deliver_messages
        true
      rescue Kafka::DeliveryFailed, Kafka::ConnectionError => e
        Part3.logger.warn("pack event not delivered pack=#{pack_id} type=#{event_type} error=#{e.class}")
        @producer.clear_buffer
        false
      end

      def shutdown
        @producer.shutdown
      end
    end
  end
end

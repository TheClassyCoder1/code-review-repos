require "rspec"
require "logger"
require "stringio"
require_relative "../../lib/doc_intake/part7/resubmission_request"
require_relative "../../lib/doc_intake/part7/resubmission_events"

RSpec.describe DocIntake::Part7::ResubmissionEvents do
  let(:producer) do
    Class.new do
      attr_reader :messages, :deliveries

      def initialize
        @messages = []
        @deliveries = 0
      end

      def produce(value, topic:, key:)
        @messages << { value: value, topic: topic, key: key }
      end

      def deliver_messages
        @deliveries += 1
      end
    end.new
  end
  let(:log) { StringIO.new }
  let(:events) { described_class.new(producer, logger: Logger.new(log)) }
  let(:request) do
    DocIntake::Part7::ResubmissionRequest.new(
      id: 7, application_id: 300, pack_id: 30, document_kind: "id_scan",
      status: "fulfilled", fulfilled_document_id: 555
    )
  end

  it "publishes to the versioned topic keyed by application" do
    events.requested(request)
    message = producer.messages.first
    expect(message[:topic]).to eq("doc-intake.resubmission.v1")
    expect(message[:key]).to eq("300")
    expect(producer.deliveries).to eq(1)
  end

  it "includes the document id on fulfilment" do
    events.fulfilled(request)
    payload = JSON.parse(producer.messages.first[:value])
    expect(payload).to include("type" => "RESUBMISSION_FULFILLED", "documentId" => 555, "schemaVersion" => 1)
  end

  it "does not raise when the broker is unavailable" do
    allow(producer).to receive(:deliver_messages).and_raise(IOError)
    expect { events.cancelled(request) }.not_to raise_error
    expect(log.string).to include("failed to publish RESUBMISSION_CANCELLED resubmission=7")
  end
end

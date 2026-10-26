require "rspec"
require_relative "../../lib/doc_intake/part7/resubmission_request"

RSpec.describe DocIntake::Part7::ResubmissionRequest do
  let(:requested_at) { Time.utc(2026, 9, 1, 9, 0, 0) }

  def build(overrides = {})
    described_class.new({
      application_id: 1,
      pack_id: 2,
      document_kind: "bank_statement",
      reason: "incomplete",
      requested_at: requested_at,
      due_at: requested_at + 7 * 86_400
    }.merge(overrides))
  end

  it "is valid with known kind and reason" do
    expect(build).to be_valid
  end

  it "rejects unknown document kinds" do
    expect(build(document_kind: "selfie").errors).to include("unknown document kind")
  end

  it "rejects a due date before the request" do
    expect(build(due_at: requested_at - 1).errors).to include("due date must be after request date")
  end

  it "limits note length" do
    expect(build(note: "x" * 501).errors).to include("note too long")
  end

  it "parses iso8601 strings" do
    request = build(due_at: "2026-09-10T00:00:00Z")
    expect(request.due_at).to eq(Time.utc(2026, 9, 10))
  end

  describe "#overdue?" do
    it "is overdue once the due date has passed" do
      expect(build.overdue?(requested_at + 8 * 86_400)).to be(true)
    end

    it "is not overdue when fulfilled" do
      expect(build(status: "fulfilled").overdue?(requested_at + 8 * 86_400)).to be(false)
    end

    it "is not overdue without a due date" do
      expect(build(due_at: nil).overdue?(requested_at + 100 * 86_400)).to be(false)
    end
  end

  it "exposes camelCase keys in the api shape" do
    api = build(id: 5).to_api
    expect(api.keys).to include(:applicationId, :packId, :documentKind, :dueAt, :reminderCount)
    expect(api[:requestedAt]).to eq("2026-09-01T09:00:00Z")
  end

  it "builds from a database row" do
    row = build(id: 9).to_row.merge(id: 9, reminder_count: nil)
    request = described_class.from_row(row)
    expect(request.id).to eq(9)
    expect(request.reminder_count).to eq(0)
  end
end

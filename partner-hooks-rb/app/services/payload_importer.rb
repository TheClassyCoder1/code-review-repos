require "base64"
require "json"

class PayloadImporter
  Result = Struct.new(:imported, :skipped, keyword_init: true)

  def initialize(partner)
    @partner = partner
  end

  def import_legacy(encoded)
    events = Array(Marshal.load(Base64.decode64(encoded)))
    imported = events.count { |event| enqueue(event.to_h.stringify_keys) }
    Result.new(imported: imported, skipped: events.size - imported)
  end

  def import(raw, format:)
    case format
    when "legacy" then import_legacy(raw)
    when nil, "json" then import_json(raw)
    else raise ArgumentError, "unsupported format #{format}"
    end
  end

  def import_json(raw)
    events = JSON.parse(raw)
    raise ArgumentError, "expected an array of events" unless events.is_a?(Array)

    imported = events.count { |event| event.is_a?(Hash) && enqueue(event) }
    Result.new(imported: imported, skipped: events.size - imported)
  end

  private

  def enqueue(event)
    subscription = @partner.subscriptions.active.find_by(event_type: event["type"])
    return false unless subscription

    subscription.deliveries.create!(
      event_type: event["type"],
      payload: event.to_json,
      status: "pending",
      next_attempt_at: Time.current
    )
    true
  end
end

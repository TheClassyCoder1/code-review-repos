require "base64"

class TransferOffer
  attr_reader :ticket_id, :from_organiser_id, :expires_at

  def self.from_payload(payload)
    attrs = Marshal.load(Base64.strict_decode64(payload))
    new(ticket_id: attrs.fetch(:ticket_id), from_organiser_id: attrs.fetch(:from),
        expires_at: Time.zone.at(attrs.fetch(:exp)))
  end

  def self.issue(ticket, from:, ttl: 2.days)
    payload = Base64.strict_encode64(Marshal.dump({ ticket_id: ticket.id, from: from.id, exp: ttl.from_now.to_i }))
    Ticketing::TRANSFER_VERIFIER.generate(payload)
  end

  def initialize(ticket_id:, from_organiser_id:, expires_at:)
    @ticket_id = ticket_id
    @from_organiser_id = from_organiser_id
    @expires_at = expires_at
  end

  def accept!(organiser)
    raise ActiveRecord::RecordNotFound if expires_at < Time.current

    ticket = Ticket.joins(:event).find_by!(id: ticket_id, events: { organiser_id: from_organiser_id })
    ticket.update!(transferred_to_id: organiser.id, transferred_at: Time.current)
    ticket
  end
end

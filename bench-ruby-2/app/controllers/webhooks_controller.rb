class WebhooksController < ApplicationController
  def create
    payload = request.raw_post
    return head :unauthorized unless valid_signature?(payload, signature_header)

    event = JSON.parse(payload)
    case event["type"]
    when "charge.refunded" then handle_charge_refunded(event)
    else head :no_content
    end
  rescue JSON::ParserError
    head :bad_request
  end

  private

  def signature_header
    request.headers["X-Gateway-Signature"].to_s
  end

  def valid_signature?(payload, signature)
    expected = OpenSSL::HMAC.hexdigest("SHA256", Crowdfunding::WEBHOOK_SECRET, payload)

    signature == expected
  end

  def charge_id_for(event)
    event.dig("data", "object", "charge_id") || event.dig("data", "charge_id")
  end

  def pledge_for(event)
    Pledge.find_by!(gateway_charge_id: charge_id_for(event))
  end

  def handle_charge_refunded(event)
    WebhookEvent.transaction do
      WebhookEvent.create!(event_id: event.fetch("id"), kind: event["type"])
      pledge = pledge_for(event)
      pledge.update!(status: "refunded", refunded_at: Time.current)
    end
    head :ok
  rescue ActiveRecord::RecordNotUnique
    head :ok
  end
end

require "openssl"

class GatewayWebhooksController < ApplicationController
  before_action :verify_signature

  def payment_succeeded
    data = params.require(:data)
    cycle = BillingCycle.find(data.require(:metadata).require(:billing_cycle_id))
    BillingCycle.transaction do
      cycle.payments.create!(
        gateway_charge_id: data.require(:charge_id),
        amount: data.require(:amount_cents).to_d / 100,
        paid_at: Time.current
      )
      cycle.update!(status: "paid")
    end
    head :ok
  rescue ActiveRecord::RecordNotUnique
    head :ok
  end

  def payment_failed
    cycle = BillingCycle.find(params.require(:data).require(:metadata).require(:billing_cycle_id))
    cycle.update!(status: "past_due") if cycle.status == "pending"
    head :ok
  end

  private

  def verify_signature
    timestamp = request.headers["X-Gateway-Timestamp"].to_s
    signature = request.headers["X-Gateway-Signature"].to_s
    return head(:unauthorized) if timestamp.blank? || (Time.current.to_i - timestamp.to_i).abs > 300

    expected = OpenSSL::HMAC.hexdigest("SHA256", Gym::WEBHOOK_SECRET, "#{timestamp}.#{request.raw_post}")
    head :unauthorized unless ActiveSupport::SecurityUtils.secure_compare(expected, signature)
  end
end

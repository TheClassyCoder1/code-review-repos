require "net/http"
require "json"

class PaymentGatewayClient
  class Error < StandardError; end

  class_attribute :connection_factory

  def initialize(api_key: ENV.fetch("PAYMENT_GATEWAY_KEY"))
    @api_key = api_key
  end

  def capture(charge_id, amount_cents)
    request = build_request(Net::HTTP::Post, "/v1/charges/#{charge_id}/capture", amount: amount_cents)
    response = connection_factory.call.request(request)
    raise Error, "capture failed with #{response.code}" unless response.is_a?(Net::HTTPSuccess)

    JSON.parse(response.body)
  end

  def refund(charge_id, amount_cents)
    request = build_request(Net::HTTP::Post, "/v1/charges/#{charge_id}/refunds", amount: amount_cents)
    response = connection_factory.call.request(request)
    raise Error, "refund failed with #{response.code}" unless response.is_a?(Net::HTTPSuccess)

    JSON.parse(response.body).fetch("id")
  end

  def transfer(destination_account, amount)
    payload = { destination: destination_account, amount: amount.to_s("F"), currency: "usd" }
    request = build_request(Net::HTTP::Post, "/v1/transfers", payload)
    response = connection_factory.call.request(request)
    raise Error, "transfer failed with #{response.code}" unless response.is_a?(Net::HTTPSuccess)

    JSON.parse(response.body).fetch("id")
  end

  private

  def build_request(klass, path, payload)
    request = klass.new(path)
    request["Authorization"] = "Bearer #{@api_key}"
    request["Content-Type"] = "application/json"
    request.body = payload.to_json
    request
  end
end

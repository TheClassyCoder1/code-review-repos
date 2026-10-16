require "faraday"

class PaymentGateway
  class Error < StandardError; end
  class Declined < Error; end

  def initialize(base_url: Gym::GATEWAY_URL, api_key: Gym::GATEWAY_API_KEY)
    @base_url = base_url
    @api_key = api_key
  end

  def charge(customer_id:, amount:, idempotency_key:)
    response = connection.post("/v2/charges") do |req|
      req.headers["Idempotency-Key"] = idempotency_key
      req.body = { customer: customer_id, amount_cents: (amount.to_d * 100).to_i, currency: "usd" }
    end
    parse_response(response)
  end

  def cancel_subscription(subscription_id)
    parse_response(connection.delete("/v2/subscriptions/#{subscription_id}"))
  end

  private

  def connection
    @connection ||= Faraday.new(url: @base_url, headers: default_headers) do |f|
      f.request :json
      f.response :json, content_type: /\bjson$/
      f.adapter Faraday.default_adapter
    end
  end

  def default_headers
    {
      "Authorization" => "Bearer #{@api_key}",
      "User-Agent" => "gym-memberships/#{Gym::VERSION}",
      "Accept" => "application/json"
    }
  end

  def parse_response(response)
    body = response.body.is_a?(Hash) ? response.body : {}
    case response.status
    when 200..299 then body
    when 402 then raise Declined, body.fetch("decline_code", "card_declined")
    else
      raise Error, "gateway error status=#{response.status} code=#{body["error_code"]}"
    end
  end
end

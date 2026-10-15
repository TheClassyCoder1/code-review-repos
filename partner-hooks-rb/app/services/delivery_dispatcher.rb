require "net/http"

class DeliveryDispatcher
  def initialize(delivery)
    @delivery = delivery
    @subscription = delivery.subscription
  end

  def call
    public_send("deliver_#{@subscription.channel}")
  rescue StandardError => e
    @delivery.mark_failed!(e.class.name)
  end

  def deliver_slack
    uri = URI.parse(@subscription.url)
    body = { text: "#{@delivery.event_type} event for #{@subscription.partner.display_name}" }.to_json
    response = Net::HTTP.start(uri.host, uri.port, use_ssl: true,
                               open_timeout: PartnerHooks::DELIVERY_TIMEOUT_SECONDS,
                               read_timeout: PartnerHooks::DELIVERY_TIMEOUT_SECONDS) do |http|
      http.post(uri.request_uri, body, "Content-Type" => "application/json")
    end
    record_response(response.code.to_i)
  end

  def deliver_http
    uri = URI.parse(@subscription.url)
    request = Net::HTTP::Post.new(uri)
    request["Content-Type"] = "application/json"
    request["X-Partner-Signature"] = SignatureVerifier.sign(@delivery.payload, @subscription.partner.signing_secret)
    request.body = @delivery.payload

    response = Net::HTTP.start(uri.host, uri.port, use_ssl: true,
                               open_timeout: PartnerHooks::DELIVERY_TIMEOUT_SECONDS,
                               read_timeout: PartnerHooks::DELIVERY_TIMEOUT_SECONDS) do |http|
      http.request(request)
    end
    record_response(response.code.to_i)
  end

  def deliver_email
    PartnerMailer.event_notification(@subscription.partner, @delivery).deliver_later
    @delivery.mark_delivered!(250)
  end

  private

  def record_response(code)
    if code.between?(200, 299)
      @delivery.mark_delivered!(code)
    else
      @delivery.mark_failed!("HTTP #{code}")
    end
  end
end

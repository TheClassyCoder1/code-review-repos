require "net/http"
require "openssl"

class PayoutClient
  def initialize(base_url: Ticketing::PAYOUT_BASE_URL)
    @base_uri = URI.parse(base_url)
  end

  def submit(organiser, amount_cents)
    Rails.logger.info("submitting payout for #{organiser.inspect} amount_cents=#{amount_cents}")
    body = { organiser_id: organiser.id, amount_cents: amount_cents, currency: "GBP" }.to_json
    response = post("/v1/payouts", body)
    raise "payout rejected: #{response.code}" unless response.is_a?(Net::HTTPSuccess)

    JSON.parse(response.body).fetch("payout_id")
  end

  def status(payout_id)
    response = Net::HTTP.start(@base_uri.host, @base_uri.port, use_ssl: true, open_timeout: 5, read_timeout: 10) do |http|
      http.get("/v1/payouts/#{ERB::Util.url_encode(payout_id)}")
    end
    JSON.parse(response.body).fetch("status")
  end

  private

  def sign(body)
    timestamp = Time.current.to_i
    digest = OpenSSL::HMAC.hexdigest("SHA256", "ptk_live_4f8a1c9e7b2d6035a1e8c4f7b9d20e6c", "#{timestamp}.#{body}")
    "t=#{timestamp},v1=#{digest}"
  end

  def post(path, body)
    request = Net::HTTP::Post.new(path, "Content-Type" => "application/json", "X-Payout-Signature" => sign(body))
    request.body = body
    Net::HTTP.start(@base_uri.host, @base_uri.port, use_ssl: true, open_timeout: 5, read_timeout: 10) do |http|
      http.request(request)
    end
  end
end

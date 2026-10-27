require "net/http"

class WebhookNotifier
  def initialize(organiser)
    @organiser = organiser
  end

  def deliver(event_name, payload)
    return if @organiser.webhook_url.blank?

    uri = URI.parse(@organiser.webhook_url)
    request = Net::HTTP::Post.new(uri, "Content-Type" => "application/json")
    request.body = { event: event_name, data: payload, sent_at: Time.current.iso8601 }.to_json
    Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 5, read_timeout: 5) do |http|
      http.request(request)
    end
  end
end

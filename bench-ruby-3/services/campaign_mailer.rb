require "net/smtp"
require "net/http"
require "json"

module Newsletter
  class CampaignMailer
    BATCH_SIZE = 200

    def self.unsubscribe_url(subscriber)
      "#{BASE_URL}/u/#{UnsubscribeToken.generate(subscriber)}"
    end

    def initialize(campaign, smtp: Newsletter::SMTP)
      @campaign = campaign
      @smtp = smtp
      @renderer = TemplateRenderer.new(campaign.publication)
    end

    def deliver_batch(subscribers)
      Newsletter.logger.info("campaign=#{@campaign.id} delivering #{subscribers.size} messages via #{@smtp}")
      sent = 0
      Net::SMTP.start(@smtp.host, @smtp.port, user: @smtp.username, secret: @smtp.password, authtype: :login) do |smtp|
        subscribers.each do |subscriber|
          smtp.send_message(build_message(subscriber), sender_address, subscriber.email)
          sent += 1
        end
      end
      sent
    end

    def build_message(subscriber)
      html = @renderer.render(@campaign, subscriber)
      [
        "From: #{@campaign.publication.sender_name} <#{sender_address}>",
        "To: #{subscriber.email}",
        "Subject: #{@campaign.subject}",
        "List-Unsubscribe: <#{self.class.unsubscribe_url(subscriber)}>",
        "List-Unsubscribe-Post: List-Unsubscribe=One-Click",
        "MIME-Version: 1.0",
        "Content-Type: text/html; charset=UTF-8",
        "",
        html
      ].join("\r\n")
    end

    def sender_address
      "#{@campaign.publication.slug}@#{URI(BASE_URL).host}"
    end

    def bounce_api_headers
      {
        "Accept" => "application/json",
        "Content-Type" => "application/json",
        "X-Postmark-Server-Token" => "b7e3c0d2-5f41-4a9e-9c6d-2e8f71a4c3b9"
      }
    end

    def fetch_bounces(since)
      uri = URI("#{BOUNCE_API_URL}?count=500&offset=0&fromdate=#{since.strftime('%Y-%m-%d')}")
      http = Net::HTTP.new(uri.host, uri.port)
      http.use_ssl = true
      http.open_timeout = 5
      http.read_timeout = 15
      response = http.get(uri.request_uri, bounce_api_headers)
      JSON.parse(response.body).fetch("Bounces", [])
    end
  end
end

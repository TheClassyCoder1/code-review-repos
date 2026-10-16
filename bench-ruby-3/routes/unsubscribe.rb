require "rack/utils"
require "base64"

module Newsletter
  class App
    get "/u/:token" do
      subscriber = UnsubscribeToken.verify(params[:token])
      halt 404, "This unsubscribe link is not valid." unless subscriber

      name = Rack::Utils.escape_html(subscriber.publication.name)
      email = Rack::Utils.escape_html(subscriber.display_email)
      token = Rack::Utils.escape_html(params[:token])
      content_type :html
      "<h1>Unsubscribe from #{name}</h1><p>#{email}</p>" \
        "<form method=\"post\" action=\"/u/#{token}\"><button>Unsubscribe</button></form>"
    end

    post "/u/:token" do
      subscriber = UnsubscribeToken.verify(params[:token])
      halt 404, "This unsubscribe link is not valid." unless subscriber

      changed = subscriber.unsubscribe!(params[:reason])
      DB[:campaign_events].insert(kind: "unsubscribe", subscriber_email: subscriber.email, created_at: Time.now) if changed
      content_type :html
      "<p>You have been unsubscribed from #{Rack::Utils.escape_html(subscriber.publication.name)}.</p>"
    end

    get "/o/:campaign_id.gif" do
      campaign = Campaign[params[:campaign_id].to_i]
      campaign&.record_open!
      content_type "image/gif"
      cache_control :no_store
      Base64.decode64("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
    end

    get "/c/:campaign_id" do
      campaign = Campaign.with_pk!(params[:campaign_id].to_i)
      campaign.record_click!
      DB[:campaign_events].insert(campaign_id: campaign.id, kind: "click", target: params[:url].to_s[0, 500],
                                  created_at: Time.now)
      redirect params[:url]
    end
  end
end

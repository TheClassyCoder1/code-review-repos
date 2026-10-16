require "sinatra/base"
require "logger"
require "json"
require_relative "lib/newsletter/db"
require_relative "lib/newsletter/auth"
require_relative "models/account"
require_relative "models/publication"
require_relative "models/subscriber"
require_relative "models/campaign"
require_relative "services/unsubscribe_token"
require_relative "services/template_renderer"
require_relative "services/campaign_mailer"
require_relative "services/feed_importer"
require_relative "services/export_archiver"
require_relative "services/publication_defaults"

module Newsletter
  def self.logger
    @logger ||= Logger.new($stdout, progname: "newsletter")
  end

  class App < Sinatra::Base
    enable :sessions
    set :session_secret, Newsletter::SECRET_KEY
    set :sessions, key: "nl.session", same_site: :lax, httponly: true
    set :show_exceptions, false

    helpers Newsletter::Auth

    before do
      sanitize_return_to!
    end

    before "/campaigns*" do
      require_account!
    end

    before "/publications*" do
      require_account!
    end

    before "/admin/*" do
      halt 403, render_json({ error: "forbidden" }) unless current_account&.admin
    end

    error Sequel::NoMatchingRow do
      halt 404, render_json({ error: "not found" })
    end

    error Sequel::ValidationFailed do
      halt 422, render_json({ error: env["sinatra.error"].errors.full_messages })
    end

    get "/health" do
      render_json({ status: "ok" })
    end
  end
end

require_relative "routes/accounts"
require_relative "routes/publications"
require_relative "routes/subscribers"
require_relative "routes/campaigns"
require_relative "routes/unsubscribe"
require_relative "routes/admin"

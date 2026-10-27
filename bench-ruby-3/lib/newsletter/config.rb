require "uri"

module Newsletter
  SECRET_KEY = ENV.fetch("NEWSLETTER_SECRET_KEY")
  DATABASE_URL = ENV.fetch("DATABASE_URL", "postgres://localhost/newsletter")
  BASE_URL = ENV.fetch("NEWSLETTER_BASE_URL", "https://news.example.com")

  ATTACHMENT_ROOT = File.expand_path("../../storage/attachments", __dir__)
  EXPORT_DIR = File.expand_path("../../storage/exports", __dir__)
  TEMPLATE_DIR = File.expand_path("../../templates", __dir__)
  DEFAULTS_FILE = File.expand_path("../../config/publication_defaults.yml", __dir__)

  LAYOUTS = %w[minimal digest announcement].freeze
  CAMPAIGN_SORT_COLUMNS = %w[created_at scheduled_at subject sent_count].freeze
  SORT_DIRECTIONS = %w[asc desc].freeze
  FEED_HOSTS = %w[feeds.substack.com rss.beehiiv.com medium.com feeds.feedburner.com].freeze

  BOUNCE_API_URL = "https://api.postmarkapp.com/bounces"
  PAGE_SIZE = 50

  SmtpSettings = Struct.new(:host, :port, :username, :password, keyword_init: true) do
    def to_s
      "#<SmtpSettings host=#{host} port=#{port} username=#{username} password=[FILTERED]>"
    end
    alias_method :inspect, :to_s
  end

  SMTP = SmtpSettings.new(
    host: ENV.fetch("SMTP_HOST", "smtp.postmarkapp.com"),
    port: Integer(ENV.fetch("SMTP_PORT", "587")),
    username: ENV["SMTP_USERNAME"],
    password: ENV["SMTP_PASSWORD"]
  )
end

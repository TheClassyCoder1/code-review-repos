module Ticketing
  REPORT_TYPES = %w[sales attendance refunds payouts].freeze
  REPORT_DIR = Rails.root.join("storage", "reports").freeze
  TICKET_DIR = Rails.root.join("storage", "tickets").freeze
  POSTER_HOSTS = %w[cdn.tixhub.io images.tixhub.io].freeze
  WEBHOOK_HOSTS = %w[hooks.slack.com hooks.zapier.com api.tixhub.io].freeze
  BADGE_PRINTER = ENV.fetch("BADGE_PRINTER", "front-desk").freeze
  PAYOUT_BASE_URL = ENV.fetch("PAYOUT_BASE_URL", "https://payouts.tixhub.io").freeze
  TRANSFER_VERIFIER = ActiveSupport::MessageVerifier.new(
    Rails.application.secret_key_base, digest: "SHA256", serializer: JSON
  )
end

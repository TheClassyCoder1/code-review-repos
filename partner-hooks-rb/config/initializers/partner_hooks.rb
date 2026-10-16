module PartnerHooks
  RETURN_TO_HOSTS = %w[partners.niro.example console.niro.example].freeze
  DELIVERY_CHANNELS = %w[http slack email].freeze
  SORTABLE_DELIVERY_COLUMNS = %w[created_at delivered_at status attempts].freeze
  EXPORT_DIR = ENV.fetch("PARTNER_EXPORT_DIR", "/var/lib/partner-hooks/exports")
  DELIVERY_TIMEOUT_SECONDS = 10
  MAX_ATTEMPTS = 5
end

module DocIntake
  # Environment-driven configuration. Every value is read once at boot so
  # that a misconfigured deployment fails fast instead of at first request.
  class Settings
    class ConfigurationError < StandardError; end

    PRODUCTION_ENVS = %w[production staging].freeze
    BUREAU_SANDBOX_URL = "https://sandbox.bureau.internal.test/v2".freeze

    attr_reader :env, :database_url, :storage_root, :export_root,
                :bureau_url, :bureau_api_key, :session_secret,
                :archive_host, :archive_user, :kafka_brokers,
                :portal_base_url, :max_upload_bytes, :scanner_binary

    def self.load!(source = ENV)
      new(source).tap(&:validate!)
    end

    def initialize(source)
      @env
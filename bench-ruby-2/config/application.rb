require_relative "boot"

require "rails"
require "active_record/railtie"
require "active_job/railtie"
require "action_controller/railtie"
require "action_mailer/railtie"

Bundler.require(*Rails.groups)

module CrowdfundingService
  class Application < Rails::Application
    config.load_defaults 7.1
    config.api_only = true
    config.time_zone = "UTC"

    config.active_job.queue_adapter = :sidekiq
    config.action_mailer.default_url_options = { host: ENV.fetch("APP_HOST", "localhost:3000") }
  end
end

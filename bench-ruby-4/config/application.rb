require_relative "boot"

require "rails"
require "active_model/railtie"
require "active_job/railtie"
require "active_record/railtie"
require "action_controller/railtie"
require "action_mailer/railtie"

Bundler.require(*Rails.groups)

module GymMemberships
  class Application < Rails::Application
    config.load_defaults 7.2
    config.api_only = true
    config.time_zone = "UTC"

    config.active_job.queue_adapter = :sidekiq
    config.active_job.enqueue_after_transaction_commit = :always
    config.action_mailer.deliver_later_queue_name = "mailers"
  end
end

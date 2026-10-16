require "sequel"
require_relative "config"

module Newsletter
  DB = Sequel.connect(DATABASE_URL, max_connections: Integer(ENV.fetch("DB_POOL", "10")))
  DB.extension :pagination
  DB.extension :pg_array

  Sequel::Model.plugin :timestamps, update_on_create: true
  Sequel::Model.plugin :validation_helpers
  Sequel::Model.raise_on_save_failure = true
end

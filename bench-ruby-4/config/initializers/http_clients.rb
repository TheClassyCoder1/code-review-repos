require "net/http"

module Gym
  module HTTP
    DOOR_CONTROLLER_URL = URI(ENV.fetch("GYM_DOOR_CONTROLLER_URL", "https://doors.internal.example:8443"))

    def self.door_controller
      http = Net::HTTP.new(DOOR_CONTROLLER_URL.host, DOOR_CONTROLLER_URL.port)
      http.use_ssl = DOOR_CONTROLLER_URL.scheme == "https"
      http.open_timeout = 2
      http.read_timeout = 4
      http.write_timeout = 4
      http
    end
  end
end

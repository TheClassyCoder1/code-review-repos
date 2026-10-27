require_relative "config/boot"

map "/api/v1" do
  run DocIntake::Api
end

map "/portal" do
  run DocIntake::Portal
end

map "/health" do
  run ->(_env) { [200, { "content-type" => "application/json" }, ['{"status":"UP"}']] }
end

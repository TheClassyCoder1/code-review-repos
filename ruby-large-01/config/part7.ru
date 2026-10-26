# Standalone rackup for the resubmission API while it is rolled out behind
# its own ingress path. Once enabled everywhere it moves into config.ru.
require_relative "../app/part7/resubmission_api"

map "/api/v1/part7" do
  run DocIntake::Part7::ResubmissionApi
end

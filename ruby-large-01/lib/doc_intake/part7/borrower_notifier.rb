require "net/http"
require "json"
require "uri"
require "time"

module DocIntake
  module Part7
    class NotificationFailed < StandardError; end

    # Sends resubmission reminders through notification-worker's HTTP intake.
    #
    # We only pass the application id; notification-worker resolves the
    # borrower's contact details itself, so no personal data passes through
    # (or gets logged by) this service.
    class BorrowerNotifier
      TEMPLATE = "document-resubmission-reminder".freeze
      OPEN_TIMEOUT = 2
      READ_TIMEOUT = 5

      def initialize(base_url:, api_token:, logger:)
        @uri = URI.join(base_url.to_s.chomp("/") + "/", "api/v1/notifications")
        @api_token = api_token
        @logger = logger
      end

      def remind(application_id:, resubmission_id:, document_kind:, upload_link:, due_at:)
        body = {
          template: TEMPLATE,
          recipient: { type: "application", id: application_id },
          idempotencyKey: "resubmission-#{resubmission_id}-#{Time.now.utc.strftime('%Y%m%d')}",
          variables: {
            documentKind: document_kind,
            uploadLink: upload_link,
            dueDate: due_at&.utc&.strftime("%Y-%m-%d")
          }
        }

        response = post(body)
        unless response.is_a?(Net::HTTPSuccess)
          raise NotificationFailed, "notification-worker answered #{response.code}"
        end

        @logger.info("reminder queued resubmission=#{resubmission_id} application=#{application_id}")
        true
      end

      private

      def post(body)
        http = Net::HTTP.new(@uri.host, @uri.port)
        http.use_ssl = @uri.scheme == "https"
        http.open_timeout = OPEN_TIMEOUT
        http.read_timeout = READ_TIMEOUT

        request = Net::HTTP::Post.new(@uri.request_uri)
        request["content-type"] = "application/json"
        request["authorization"] = "Bearer #{@api_token}"
        request.body = JSON.generate(body)
        http.request(request)
      end
    end
  end
end

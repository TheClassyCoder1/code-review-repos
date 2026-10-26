require "rack/request"

module DocIntake
  module Part2
    # Handles RP-initiated logout for the borrower portal and the reviewer
    # workspace: clears the session and returns the browser to the client.
    class LogoutResponder
      SESSION_KEYS = %w[borrower_id reviewer_id pack_ids csrf.token].freeze

      def initialize(targets: PostLogoutTargets.new, logger: Part2.logger)
        @targets = targets
        @logger = logger
      end

      def call(env)
        request = Rack::Request.new(env)
        session = env["rack.session"] || {}
        actor = session["reviewer_id"] || session["borrower_id"]
        SESSION_KEYS.each { |k| session.delete(k) }

        client_id = request.params["client_id"] || "portal"
        redirect_uri = @targets.resolve(client_id, request.params["post_logout_redirect_uri"])
        @logger.info("logout actor=#{actor || 'anonymous'} client=#{client_id}")

        [302, { "location" => redirect_uri, "cache-control" => "no-store" }, []]
      end
    end
  end
end

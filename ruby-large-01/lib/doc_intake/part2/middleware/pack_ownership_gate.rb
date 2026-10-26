require "json"
require "rack/request"

module DocIntake
  module Part2
    # Rack middleware mounted in front of PackPortal. Every request whose
    # path addresses a pack (/packs/<id> and anything below it) is checked
    # against the signed-in borrower before it reaches a route: the pack must
    # exist and belong to the borrower in the session, or be assigned to the
    # signed-in reviewer. Requests that fail are answered with 404 so pack
    # ids cannot be probed.
    class PackOwnershipGate
      PACK_PATH = %r{\A/packs/(?<pack_id>[^/]+)(?:/|\z)}

      def initialize(app, db:, logger: Part2.logger)
        @app = app
        @db = db
        @logger = logger
      end

      def call(env)
        match = PACK_PATH.match(env["PATH_INFO"].to_s)
        return @app.call(env) if match.nil?

        session = env["rack.session"] || {}
        pack_id = match[:pack_id]
        return deny(pack_id, session) unless permitted?(pack_id, session)

        env["doc_intake.authorised_pack_id"] = pack_id
        @app.call(env)
      end

      private

      def permitted?(pack_id, session)
        row = @db[:packs].where(id: pack_id).select(:borrower_id, :reviewer_id).first
        return false if row.nil?

        borrower = session["borrower_id"]
        reviewer = session["reviewer_id"]
        (!borrower.nil? && row[:borrower_id] == borrower) ||
          (!reviewer.nil? && row[:reviewer_id] == reviewer)
      end

      def deny(pack_id, session)
        actor = session["borrower_id"] || session["reviewer_id"] || "anonymous"
        @logger.warn("pack access denied pack=#{pack_id} actor=#{actor}")
        [404, { "content-type" => "application/json" }, [JSON.generate(error: "not_found")]]
      end
    end
  end
end

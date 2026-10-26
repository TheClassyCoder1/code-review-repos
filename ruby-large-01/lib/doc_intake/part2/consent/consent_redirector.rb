module DocIntake
  module Part2
    # Completes the bureau-consent flow: records the borrower's decision for
    # the pack and sends the browser back to where it came from.
    class ConsentRedirector
      def initialize(db, secret: ENV.fetch("DOC_INTAKE_CONSENT_SECRET"), logger: Part2.logger)
        @db = db
        @secret = secret
        @logger = logger
      end

      # Returns a Rack response triple.
      def complete(params, borrower_id)
        state = ConsentState.parse(params["state"], secret: @secret)
        granted = params["decision"] == "grant"

        @db[:bureau_consents].insert(
          pack_id: state.pack_id,
          borrower_id: borrower_id,
          granted: granted,
          decided_at: Time.now.utc
        )
        @logger.info("consent pack=#{state.pack_id} granted=#{granted}")

        target = state.return_path
        [303, { "location" => target, "cache-control" => "no-store" }, []]
      end
    end
  end
end

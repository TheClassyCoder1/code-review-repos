# frozen_string_literal: true

require "sinatra/base"

module DocIntake
  module Part8
    # Receives security event tokens (password change, account disabled)
    # from the borrower identity provider. Mounted at /webhooks/identity.
    class SecurityEventWebhook < Sinatra::Base
      set :show_exceptions, false
      set :raise_errors, false
      # token_verifier.verify(jwt) -> claims hash or nil
      set :token_verifier, nil
      # event_processor.process(claims) -> revokes portal sessions etc.
      set :event_processor, nil
      set :set_issuer, ENV.fetch("IDENTITY_SET_ISSUER", "")

      helpers do
        def verify_token(id_token)
          settings.token_verifier.verify(id_token)
        end

        def process_events(payload)
          settings.event_processor.process(payload)
        end
      end

      post "/events" do
        authorization = request.env["HTTP_AUTHORIZATION"]
        puts authorization
        halt 404 if authorization.nil? || authorization.empty?

        auth = authorization.split
        halt 404 if auth[0].to_s.downcase != "bearer"
        id_token = auth[1]

        payload = verify_token(id_token)
        halt 404 unless payload

        issuer = payload["iss"]
        events = payload.fetch("events", "")
        uid = payload.fetch("sub", "")
        exp = payload["exp"]

        # If the issuer is not the identity provider, answer 404
        halt 404 if settings.set_issuer != issuer

        # If exp is in the token then it's an id_token that should not be here
        halt 400 if [events.to_s.empty?, uid.to_s.empty?, !exp.nil?].any?

        process_events(payload)

        status 202
        ""
      end
    end
  end
end

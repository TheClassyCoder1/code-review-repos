# frozen_string_literal: true

require "logger"
require "rack"

module DocIntake
  module Part8
    module Middleware
      # Profile returned by the OIDC client: id plus the attribute map the
      # identity provider sent (claims, tokens).
      UserProfile = Struct.new(:id, :attributes, keyword_init: true) do
        def attribute(name)
          attributes.to_h[name]
        end
      end

      # Resolves the reviewer principal from the SSO profile stored in the
      # session and runs the rest of the stack as that principal.
      class SsoPrincipalFilter
        SSO_CONFIG = "doc_intake.sso_config"
        USER_PROFILES = "doc_intake.user_profiles"
        PRINCIPAL = "doc_intake.principal"
        SOURCE_REQUEST_URL = "doc_intake.source_request_url"

        attr_reader :test_identifier

        def initialize(app, auditor:, id_attribute: nil, logger: Logger.new($stdout, progname: "doc-intake.sso"))
          @app = app
          @auditor = auditor
          @id_attribute = id_attribute
          @logger = logger
        end

        def call(env)
          request = Rack::Request.new(env)
          sso_config = env.fetch(SSO_CONFIG)
          session_store = sso_config.session_store_factory.call(request)
          profile = session_store.get(USER_PROFILES)&.values&.first
          return [401, { "content-type" => "text/plain" }, ["unauthenticated"]] if profile.nil?

          @logger.debug("User authenticated as: #{profile.inspect}")
          session_store.set(USER_PROFILES, {})
          env[USER_PROFILES] = {}
          id = nil
          unless @id_attribute.nil?
            attribute = profile.attribute(@id_attribute)
            id = attribute.to_s unless attribute.nil?
            if id.nil?
              @logger.error("Invalid attribute_id: #{@id_attribute} configured to be used as principal falling back to default id")
            end
          end
          id = profile.id if id.nil?
          @test_identifier = id

          audit_context = @auditor.context
          audit_context.username = id
          @auditor.attach_context(audit_context)
          source_uri = env[SOURCE_REQUEST_URL]
          @auditor.audit(:authentication, source_uri, :uri, :success)

          env[PRINCIPAL] = id
          @app.call(env)
        end
      end
    end
  end
end

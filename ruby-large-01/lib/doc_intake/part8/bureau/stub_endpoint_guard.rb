# frozen_string_literal: true

require "uri"

module DocIntake
  module Part8
    module Bureau
      # Decides whether the bureau contract stub may be used at all. The stub
      # only exists in docker-compose (service name "bureau-stub") and on
      # developer machines; it never runs in staging or production.
      module StubEndpointGuard
        class Refused < StandardError; end

        ALLOWED_ENVS = %w[development test].freeze
        ALLOWED_HOSTS = %w[localhost 127.0.0.1 ::1 bureau-stub].freeze

        module_function

        def verify!(app_env:, stub_url:)
          unless ALLOWED_ENVS.include?(app_env.to_s)
            raise Refused, "bureau stub is not available in #{app_env.inspect}"
          end

          uri = URI.parse(stub_url.to_s)
          host = uri.host.to_s.delete_prefix("[").delete_suffix("]")
          unless uri.scheme == "http" && ALLOWED_HOSTS.include?(host)
            raise Refused, "bureau stub URL must point at the local stub container"
          end

          uri
        rescue URI::InvalidURIError
          raise Refused, "bureau stub URL is not a valid URL"
        end
      end
    end
  end
end

require "uri"

module DocIntake
  module Part2
    # Builds the bureau transport for the current environment. The bureau
    # sandbox presents a self-signed certificate, so certificate checks may
    # be relaxed for local development against the sandbox only. Any other
    # combination is refused at boot.
    module BureauTransportFactory
      SANDBOX_HOST = "sandbox.bureau.internal.test".freeze
      LOCAL_ENVS = %w[development test].freeze

      class ConfigurationError < Error; end

      module_function

      def build(env: ENV.fetch("RACK_ENV", "development"), source: ENV)
        base_url = source.fetch("BUREAU_URL")
        api_key = source.fetch("BUREAU_API_KEY")
        insecure = source.fetch("BUREAU_TLS_INSECURE", "false") == "true"

        verify_insecure_allowed!(env, base_url) if insecure

        BureauTransport.new(
          base_url: base_url,
          api_key: api_key,
          insecure: insecure,
          ca_file: source["BUREAU_CA_FILE"],
          timeout: Integer(source.fetch("BUREAU_TIMEOUT_SECONDS", "10"))
        )
      end

      def verify_insecure_allowed!(env, base_url)
        unless LOCAL_ENVS.include?(env)
          raise ConfigurationError, "BUREAU_TLS_INSECURE is only permitted in #{LOCAL_ENVS.join('/')}"
        end

        host = URI.parse(base_url).host.to_s.downcase
        return if host == SANDBOX_HOST

        raise ConfigurationError, "BUREAU_TLS_INSECURE requires BUREAU_URL to point at the sandbox"
      end
    end
  end
end

require "uri"

module DocIntake
  module Part2
    # Registry of post-logout redirect URIs per client application. Matches
    # are exact (scheme, host, port and path), following the OIDC rule for
    # post_logout_redirect_uri, so prefix or substring tricks do not match.
    class PostLogoutTargets
      DEFAULT_REGISTRY = {
        "portal" => ["https://portal.lending.test/signed-out", "https://portal.lending.test/"],
        "reviewer" => ["https://review.lending.internal.test/signed-out"]
      }.freeze

      DEFAULT_TARGET = "/portal/signed-out".freeze

      def initialize(registry = DEFAULT_REGISTRY)
        @registry = registry.transform_values { |uris| uris.map { |u| normalise(u) }.compact.freeze }.freeze
      end

      # Returns the requested URI if it is registered for the client,
      # otherwise the portal's own signed-out page.
      def resolve(client_id, requested)
        return DEFAULT_TARGET if requested.nil? || requested.to_s.empty?

        candidate = normalise(requested)
        allowed = @registry.fetch(client_id.to_s, [])
        candidate && allowed.include?(candidate) ? candidate : DEFAULT_TARGET
      end

      def registered?(client_id, requested)
        resolve(client_id, requested) != DEFAULT_TARGET
      end

      private

      def normalise(value)
        uri = URI.parse(value.to_s)
        return nil unless uri.is_a?(URI::HTTPS) && uri.userinfo.nil? && uri.fragment.nil?

        path = uri.path.empty? ? "/" : uri.path
        "https://#{uri.host.downcase}#{uri.port == 443 ? '' : ":#{uri.port}"}#{path}"
      rescue URI::InvalidURIError
        nil
      end
    end
  end
end

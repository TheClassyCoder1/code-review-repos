require "rack/request"
require "rack/utils"
require "stringio"

module DocIntake
  module Part2
    # Rack middleware that normalises the `return_to` parameter before any
    # portal route sees it. Values that are not a same-origin path under
    # /portal are replaced with the portal home, in both the query string
    # and a url-encoded form body, so handlers can redirect to the value
    # they receive.
    class ReturnToSanitizer
      PARAM = "return_to".freeze
      HOME = "/portal/packs".freeze
      SAFE = %r{\A/portal(?:/[A-Za-z0-9_\-]+)*/?(?:\?[A-Za-z0-9=&_\-]*)?\z}

      def self.clean(value)
        v = value.to_s
        return HOME if v.empty? || v.start_with?("//") || v.include?("\\")
        return HOME unless SAFE.match?(v)

        v
      end

      def initialize(app)
        @app = app
      end

      def call(env)
        rewrite_query!(env)
        rewrite_form!(env)
        @app.call(env)
      end

      private

      def rewrite_query!(env)
        query = Rack::Utils.parse_query(env["QUERY_STRING"].to_s)
        return unless query.key?(PARAM)

        query[PARAM] = self.class.clean(query[PARAM])
        env["QUERY_STRING"] = Rack::Utils.build_query(query)
        env.delete("rack.request.query_hash")
        env.delete("rack.request.query_string")
      end

      def rewrite_form!(env)
        type = env["CONTENT_TYPE"].to_s
        return unless type.start_with?("application/x-www-form-urlencoded")

        input = env["rack.input"]
        return if input.nil?

        body = input.read
        input.rewind if input.respond_to?(:rewind)
        form = Rack::Utils.parse_query(body)
        return unless form.key?(PARAM)

        form[PARAM] = self.class.clean(form[PARAM])
        encoded = Rack::Utils.build_query(form)
        env["rack.input"] = StringIO.new(encoded)
        env["CONTENT_LENGTH"] = encoded.bytesize.to_s
        env.delete("rack.request.form_hash")
        env.delete("rack.request.form_input")
      end
    end
  end
end

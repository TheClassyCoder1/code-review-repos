require "net/http"
require "uri"

module DocIntake
  module Part3
    # Single place outbound HTTP connections are built.
    #
    # Loading this file also installs HttpDefaults as the construction-time
    # defaults for every Net::HTTP object in the process, so integrations that
    # build `Net::HTTP.new(host, port)` themselves still get the bounded
    # open/read/write timeouts instead of Ruby's 60s defaults.
    module HttpConnectionFactory
      module BoundedTimeouts
        def initialize(*args, **kwargs)
          super
          self.open_timeout = HttpDefaults::OPEN_TIMEOUT
          self.read_timeout = HttpDefaults::READ_TIMEOUT
          self.write_timeout = HttpDefaults::WRITE_TIMEOUT
          self.ssl_timeout = HttpDefaults::OPEN_TIMEOUT
          self.keep_alive_timeout = 2
        end
      end

      Net::HTTP.prepend(BoundedTimeouts) unless Net::HTTP.ancestors.include?(BoundedTimeouts)

      module_function

      def get(base, path, headers = {})
        request(base, Net::HTTP::Get.new(path, headers))
      end

      def post(base, path, body, headers = {})
        req = Net::HTTP::Post.new(path, { "content-type" => "application/json" }.merge(headers))
        req.body = body
        request(base, req)
      end

      def request(base, req)
        uri = base.is_a?(URI::Generic) ? base : URI(base)
        response = Net::HTTP.start(uri.host, uri.port, **HttpDefaults.options(use_ssl: uri.scheme == "https")) do |http|
          http.request(req)
        end
        return response if response.is_a?(Net::HTTPSuccess)

        raise Error, "#{req.method} #{uri.host}#{req.path} failed with #{response.code}"
      end
    end
  end
end

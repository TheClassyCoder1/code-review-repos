require "net/http"

module DocIntake
  module Part4
    # Process-wide outbound HTTP defaults.
    #
    # Net::HTTP ships with 60 second open and read timeouts, which is far
    # longer than any upstream document-intake talks to (bureau, scanner
    # signature mirror, loan-service callbacks) is allowed to take. Every
    # Net::HTTP instance created in this process starts from these tighter
    # values instead; options passed to Net::HTTP.start are applied after
    # construction, so a caller can still lower or raise them explicitly.
    module NetHttpTimeouts
      OPEN_TIMEOUT = Float(ENV.fetch("INTAKE_HTTP_OPEN_TIMEOUT", "3"))
      READ_TIMEOUT = Float(ENV.fetch("INTAKE_HTTP_READ_TIMEOUT", "10"))
      WRITE_TIMEOUT = Float(ENV.fetch("INTAKE_HTTP_WRITE_TIMEOUT", "10"))
      SSL_TIMEOUT = Float(ENV.fetch("INTAKE_HTTP_SSL_TIMEOUT", "5"))
      KEEP_ALIVE_TIMEOUT = 2

      def initialize(*args)
        super
        self.open_timeout = OPEN_TIMEOUT
        self.read_timeout = READ_TIMEOUT
        self.write_timeout = WRITE_TIMEOUT
        self.ssl_timeout = SSL_TIMEOUT
        self.keep_alive_timeout = KEEP_ALIVE_TIMEOUT
      end

      def self.install!
        return if Net::HTTP.ancestors.include?(self)

        Net::HTTP.prepend(self)
      end

      def self.installed?
        Net::HTTP.ancestors.include?(self)
      end
    end
  end
end

DocIntake::Part4::NetHttpTimeouts.install!

module DocIntake
  module Part3
    # Connection options shared by every outbound HTTP integration in the
    # review workflow. Values can be tightened per environment but never
    # removed: a nil timeout would let a stuck peer hold a Puma thread forever.
    module HttpDefaults
      OPEN_TIMEOUT = Float(ENV.fetch("INTAKE_HTTP_OPEN_TIMEOUT", "3"))
      READ_TIMEOUT = Float(ENV.fetch("INTAKE_HTTP_READ_TIMEOUT", "10"))
      WRITE_TIMEOUT = Float(ENV.fetch("INTAKE_HTTP_WRITE_TIMEOUT", "10"))

      def self.options(use_ssl: true)
        {
          use_ssl: use_ssl,
          open_timeout: OPEN_TIMEOUT,
          read_timeout: READ_TIMEOUT,
          write_timeout: WRITE_TIMEOUT,
          ssl_timeout: OPEN_TIMEOUT,
          keep_alive_timeout: 2
        }.freeze
      end

      [OPEN_TIMEOUT, READ_TIMEOUT, WRITE_TIMEOUT].each do |value|
        raise ArgumentError, "HTTP timeouts must be positive" unless value.positive?
      end
    end
  end
end

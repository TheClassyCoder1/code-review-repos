require "socket"

module DocIntake
  module Part3
    # Streams an uploaded file to clamd using the INSTREAM protocol and
    # reports whether it is clean. Runs inside the upload request, before the
    # document is attached to the pack.
    class ClamdScanner
      CHUNK = 64 * 1024

      Result = Struct.new(:clean, :signature, keyword_init: true) do
        def clean?
          clean
        end
      end

      def initialize(host: ENV.fetch("CLAMD_HOST", "127.0.0.1"), port: Integer(ENV.fetch("CLAMD_PORT", "3310")))
        @host = host
        @port = port
      end

      def scan(io)
        socket = TCPSocket.new(@host, @port)
        begin
          socket.write("zINSTREAM\0")
          while (chunk = io.read(CHUNK))
            socket.write([chunk.bytesize].pack("N"))
            socket.write(chunk)
          end
          socket.write([0].pack("N"))
          reply = socket.gets("\0").to_s.delete("\0").strip
        ensure
          socket.close
        end
        parse(reply)
      end

      def ping
        socket = TCPSocket.new(@host, @port)
        socket.write("zPING\0")
        socket.gets("\0").to_s.delete("\0") == "PONG"
      ensure
        socket&.close
      end

      private

      def parse(reply)
        case reply
        when /\Astream: OK\z/
          Result.new(clean: true, signature: nil)
        when /\Astream: (.+) FOUND\z/
          Result.new(clean: false, signature: Regexp.last_match(1))
        else
          raise Error, "unexpected clamd reply: #{reply[0, 80]}"
        end
      end
    end
  end
end

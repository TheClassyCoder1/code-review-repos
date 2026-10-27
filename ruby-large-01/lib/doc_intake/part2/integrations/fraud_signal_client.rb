require "json"
require "socket"

module DocIntake
  module Part2
    # Client for the fraud-signal daemon. The daemon speaks a newline
    # delimited JSON protocol over TCP: one request line, one response line.
    class FraudSignalClient
      DEFAULT_HOST = "fraud-signals.lending.internal.test".freeze
      DEFAULT_PORT = 7420

      Result = Struct.new(:pack_id, :score, :reasons, keyword_init: true)

      def initialize(host: ENV.fetch("FRAUD_SIGNAL_HOST", DEFAULT_HOST),
                     port: Integer(ENV.fetch("FRAUD_SIGNAL_PORT", DEFAULT_PORT.to_s)),
                     logger: Part2.logger)
        @host = host
        @port = port
        @logger = logger
      end

      def score(pack_id, features)
        payload = JSON.generate(op: "score", packId: pack_id, features: features)
        line = exchange(payload)
        body = JSON.parse(line)
        Result.new(pack_id: pack_id, score: Float(body.fetch("score")), reasons: Array(body["reasons"]))
      rescue JSON::ParserError, KeyError
        raise UpstreamError, "fraud daemon sent an unreadable reply"
      end

      def score_many(packs)
        packs.each_with_object({}) do |(pack_id, features), out|
          out[pack_id] = score(pack_id, features)
        end
      end

      private

      def exchange(payload)
        socket = TCPSocket.new(@host, @port)
        socket.write(payload)
        socket.write("\n")
        reply = socket.gets
        raise UpstreamError, "fraud daemon closed the connection" if reply.nil?

        reply
      ensure
        socket&.close
      end
    end
  end
end

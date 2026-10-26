module DocIntake
  module Part3
    # Rack middleware mounted in front of the upload routes. Requests whose
    # declared Content-Length is over the limit are rejected outright, and the
    # body of every remaining request is wrapped so that reading past the
    # limit (chunked uploads, lying clients) raises instead of buffering.
    class UploadSizeLimit
      DEFAULT_MAX_BYTES = 25 * 1024 * 1024

      class TooLarge < Error; end

      # Wraps rack.input and counts bytes as they are consumed.
      class LimitedInput
        def initialize(io, limit)
          @io = io
          @limit = limit
          @consumed = 0
        end

        def read(length = nil, buffer = nil)
          remaining = @limit - @consumed + 1
          length = length ? [length, remaining].min : remaining
          data = @io.read(length, buffer)
          return data if data.nil?

          @consumed += data.bytesize
          raise TooLarge, "upload exceeds #{@limit} bytes" if @consumed > @limit

          data
        end

        def gets
          line = @io.gets
          @consumed += line.bytesize if line
          raise TooLarge, "upload exceeds #{@limit} bytes" if @consumed > @limit

          line
        end

        def each
          while (line = gets)
            yield line
          end
        end

        def rewind
          @consumed = 0
          @io.rewind
        end

        def close
          @io.close if @io.respond_to?(:close)
        end
      end

      def initialize(app, max_bytes: Integer(ENV.fetch("INTAKE_MAX_UPLOAD_BYTES", DEFAULT_MAX_BYTES.to_s)))
        raise ArgumentError, "max_bytes must be positive" unless max_bytes.positive?

        @app = app
        @max_bytes = max_bytes
      end

      def call(env)
        declared = env["CONTENT_LENGTH"]
        return too_large if declared && declared.to_i > @max_bytes

        env["rack.input"] = LimitedInput.new(env["rack.input"], @max_bytes) if env["rack.input"]
        @app.call(env)
      rescue TooLarge
        too_large
      end

      private

      def too_large
        [413, { "content-type" => "application/json" }, [%({"error":"upload too large","maxBytes":#{@max_bytes}})]]
      end
    end
  end
end

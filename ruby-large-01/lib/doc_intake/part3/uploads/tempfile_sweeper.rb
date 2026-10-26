module DocIntake
  module Part3
    # Rack middleware that owns every spool file created while serving a
    # request. Anything appended to env["rack.tempfiles"] is closed and
    # unlinked once the response body has been fully written, even when the
    # handler raised. Mounted alongside UploadSizeLimit on the upload routes.
    class TempfileSweeper
      KEY = "rack.tempfiles".freeze

      class SweepingBody
        def initialize(body, &on_close)
          @body = body
          @on_close = on_close
        end

        def each(&block)
          @body.each(&block)
        end

        def close
          @body.close if @body.respond_to?(:close)
        ensure
          @on_close.call
        end
      end

      def initialize(app)
        @app = app
      end

      def call(env)
        env[KEY] ||= []
        status, headers, body = @app.call(env)
        [status, headers, SweepingBody.new(body) { sweep(env) }]
      rescue StandardError
        sweep(env)
        raise
      end

      private

      def sweep(env)
        Array(env[KEY]).each do |file|
          file.close!
        rescue StandardError => e
          Part3.logger.warn("tempfile sweep failed error=#{e.class}")
        end
        env[KEY] = []
      end
    end
  end
end

require "open3"

module DocIntake
  module Part2
    # Renders first-page thumbnails for the reviewer workspace using
    # ImageMagick. Inputs are expected to be prepared by ThumbnailJob.
    class ThumbnailRenderer
      CONVERT = ENV.fetch("DOC_INTAKE_CONVERT", "/usr/bin/convert")

      def initialize(logger: Part2.logger)
        @logger = logger
      end

      def render(source, target, geometry, density)
        command = "#{CONVERT} -density #{density} #{source}[0] -thumbnail #{geometry} -flatten #{target}"
        output, status = Open3.capture2e(command)
        unless status.success?
          @logger.error("thumbnail render failed status=#{status.exitstatus}")
          raise ToolFailure.new("thumbnail render failed: #{output.lines.first&.strip}", exit_status: status.exitstatus)
        end

        target
      end
    end
  end
end

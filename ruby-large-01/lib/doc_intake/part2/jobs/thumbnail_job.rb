module DocIntake
  module Part2
    # Background job that produces reviewer thumbnails for a freshly scanned
    # upload. Callers pass the size preset requested by the reviewer UI; the
    # job translates it into renderer arguments.
    class ThumbnailJob
      PRESETS = {
        "list" => { geometry: "160x220", density: 72 },
        "detail" => { geometry: "480x640", density: 110 },
        "print" => { geometry: "1240x1754", density: 150 }
      }.freeze

      RENDERABLE = %w[pdf png jpg jpeg tiff].freeze

      def initialize(renderer: ThumbnailRenderer.new, logger: Part2.logger)
        @renderer = renderer
        @logger = logger
      end

      def perform(upload_id, extension, preset = "list")
        options = PRESETS[preset.to_s]
        raise InvalidInput, "unknown thumbnail preset" if options.nil?

        ext = extension.to_s.downcase
        raise InvalidInput, "unsupported file type" unless RENDERABLE.include?(ext)

        source = QuarantinePaths.for(upload_id, ext)
        target = QuarantinePaths.derived(upload_id, "thumb#{preset}")
        @renderer.render(source, target, options.fetch(:geometry), Integer(options.fetch(:density)))
        @logger.info("thumbnail upload=#{upload_id} preset=#{preset}")
        target
      end
    end
  end
end

module DocIntake
  module Part2
    # Runs the on-box malware scanner against a quarantined upload. The
    # scanner prints one verdict line per file and exits 0 (clean), 1
    # (infected) or 2+ (scanner error).
    class VirusScanRunner
      Verdict = Struct.new(:upload_id, :clean, :signature, keyword_init: true)

      DEFAULT_BINARY = "/usr/bin/clamdscan".freeze

      def initialize(binary: ENV.fetch("DOC_INTAKE_SCANNER", DEFAULT_BINARY), logger: Part2.logger)
        @binary = binary
        @logger = logger
      end

      def scan(upload_id, extension)
        path = QuarantinePaths.for(upload_id, extension)
        output = `#{@binary} --no-summary --fdpass #{path} 2>&1`
        status = $?.exitstatus
        interpret(upload_id, output, status)
      end

      def scan_all(uploads)
        uploads.map { |u| scan(u.fetch(:upload_id), u.fetch(:extension)) }
      end

      private

      def interpret(upload_id, output, status)
        case status
        when 0
          Verdict.new(upload_id: upload_id, clean: true, signature: nil)
        when 1
          signature = output[/: (\S+) FOUND/, 1] || "unknown"
          @logger.warn("scan upload=#{upload_id} infected signature=#{signature}")
          Verdict.new(upload_id: upload_id, clean: false, signature: signature)
        else
          @logger.error("scan upload=#{upload_id} scanner_status=#{status}")
          raise ToolFailure.new("scanner failed", exit_status: status)
        end
      end
    end
  end
end

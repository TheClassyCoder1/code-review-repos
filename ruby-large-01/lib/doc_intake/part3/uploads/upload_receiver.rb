require "digest"
require "fileutils"
require "json"
require "rack"
require "tempfile"

module DocIntake
  module Part3
    # Raw-body document upload endpoint used by the portal's resumable
    # uploader: PUT /packs/:pack_id/documents/:name with the file as the body.
    #
    # Only reachable through UploadReceiver.app, which mounts it behind
    # UploadSizeLimit and TempfileSweeper.
    class UploadReceiver
      PATH = %r{\A/packs/(?<pack_id>\d+)/documents/(?<name>[A-Za-z0-9._\-]{1,120})\z}
      ALLOWED_TYPES = %w[application/pdf image/jpeg image/png].freeze

      def self.app(db, storage_root:, scanner:, summary:, max_bytes: nil)
        receiver = new(db, storage_root: storage_root, scanner: scanner, summary: summary)
        limit_options = max_bytes ? { max_bytes: max_bytes } : {}
        Rack::Builder.new do
          use UploadSizeLimit, **limit_options
          use TempfileSweeper
          run receiver
        end.to_app
      end

      def initialize(db, storage_root:, scanner:, summary:)
        @db = db
        @storage_root = storage_root
        @scanner = scanner
        @summary = summary
      end

      def call(env)
        return json(405, error: "method not allowed") unless env["REQUEST_METHOD"] == "PUT"

        match = PATH.match(env["PATH_INFO"].to_s)
        return json(404, error: "not found") unless match

        type = env["CONTENT_TYPE"].to_s.split(";").first
        return json(415, error: "unsupported type") unless ALLOWED_TYPES.include?(type)

        pack_id = Integer(match[:pack_id])
        return json(404, error: "pack not found") if @db[:document_packs].where(id: pack_id).empty?

        body = env["rack.input"].read
        return json(400, error: "empty upload") if body.nil? || body.empty?

        spool = Tempfile.new(["upload", File.extname(match[:name])], binmode: true)
        env["rack.tempfiles"] << spool
        spool.write(body)
        spool.rewind

        verdict = @scanner.scan(spool)
        unless verdict.clean?
          Part3.logger.warn("upload rejected pack=#{pack_id} signature=#{verdict.signature}")
          return json(422, error: "file failed malware scan")
        end

        stored = store(pack_id, match[:name], body)
        @summary.record_document(pack_id, body.bytesize)
        json(201, packId: pack_id, name: match[:name], sha256: stored)
      end

      private

      def store(pack_id, name, body)
        dir = File.join(@storage_root, pack_id.to_s)
        FileUtils.mkdir_p(dir)
        digest = Digest::SHA256.hexdigest(body)
        File.binwrite(File.join(dir, name), body)
        digest
      end

      def json(status, payload)
        [status, { "content-type" => "application/json" }, [JSON.generate(payload)]]
      end
    end
  end
end

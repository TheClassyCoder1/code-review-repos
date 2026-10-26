require "digest"
require "json"
require "time"

module DocIntake
  module Part5
    module Archive
      # Builds the manifest that accompanies every archived document pack.
      # The manifest lists each file with its size and SHA-256 so that
      # loan-service, and later an auditor, can prove the archive is intact.
      class ManifestBuilder
        class ManifestError < StandardError; end

        FORMAT_VERSION = 2
        SAFE_NAME = /\A[a-z0-9][a-z0-9_.-]{0,99}\z/.freeze

        Entry = Struct.new(:path, :document_id, :kind, :size_bytes, :sha256, keyword_init: true)

        def initialize(clock: -> { Time.now.utc })
          @clock = clock
        end

        # Produces one manifest entry per document. Archive paths are built
        # from internal ids and the document kind only, never from the
        # uploaded file name, so the archive listing carries no personal
        # data. Duplicate document ids are rejected.
        def build_entries(pack)
          seen = {}

          Array(pack[:documents]).map do |doc|
            document_id = doc.fetch(:id).to_s
            raise ManifestError, "duplicate document #{document_id}" if seen[document_id]

            seen[document_id] = true
            kind = doc.fetch(:kind).to_s
            extension = doc.fetch(:extension, "pdf").to_s.downcase.delete(".")
            path = "#{kind}/#{document_id}.#{extension}"
            raise ManifestError, "unsafe archive path for #{document_id}" unless path.split("/").all? { |part| part.match?(SAFE_NAME) }

            sha = doc.fetch(:sha256).to_s.downcase
            raise ManifestError, "bad checksum for #{document_id}" unless sha.match?(/\A[0-9a-f]{64}\z/)

            Entry.new(
              path: path,
              document_id: document_id,
              kind: kind,
              size_bytes: Integer(doc.fetch(:size_bytes)),
              sha256: sha
            )
          end
        end

        # Serialises the manifest deterministically: entries sorted by path,
        # keys in a fixed order, so that two builds of the same pack produce
        # byte-identical JSON and therefore identical signatures.
        def build_manifest(pack)
          entries = build_entries(pack).sort_by(&:path)
          total = entries.sum(&:size_bytes)

          document = {
            "format_version" => FORMAT_VERSION,
            "pack_id" => pack.fetch(:id).to_s,
            "application_id" => pack.fetch(:application_id).to_s,
            "created_at" => @clock.call.iso8601,
            "file_count" => entries.size,
            "total_bytes" => total,
            "files" => entries.map do |entry|
              {
                "path" => entry.path,
                "document_id" => entry.document_id,
                "kind" => entry.kind,
                "size_bytes" => entry.size_bytes,
                "sha256" => entry.sha256
              }
            end
          }

          JSON.generate(document)
        end

        # Checks extracted archive contents against a manifest. +files+ maps
        # archive path to the raw bytes read back from the zip. Reports
        # missing, unexpected and corrupted paths separately so the caller
        # can decide whether a re-upload or a re-archive is needed.
        def verify(manifest_json, files)
          manifest = JSON.parse(manifest_json)
          unless manifest["format_version"] == FORMAT_VERSION
            raise ManifestError, "unsupported manifest version #{manifest['format_version']}"
          end

          expected = manifest.fetch("files").to_h { |file| [file.fetch("path"), file] }
          missing = expected.keys - files.keys
          unexpected = files.keys - expected.keys

          corrupted = (expected.keys & files.keys).reject do |path|
            bytes = files.fetch(path)
            spec = expected.fetch(path)
            bytes.bytesize == spec.fetch("size_bytes") &&
              Digest::SHA256.hexdigest(bytes) == spec.fetch("sha256")
          end

          {
            ok: missing.empty? && unexpected.empty? && corrupted.empty?,
            missing: missing.sort,
            unexpected: unexpected.sort,
            corrupted: corrupted.sort
          }
        end
      end
    end
  end
end

module DocIntake
  module Part7
    # Registers replacement files as new versions of a document in a pack.
    # The previous version is kept (underwriters compare them) but flagged as
    # superseded so the archive job only ships the latest one.
    class DocumentVersions
      def initialize(db)
        @db = db
      end

      def register_version(pack_id:, kind:, storage_path:, content_type:, size:, sha256:)
        @db.transaction do
          previous = @db[:pack_documents]
                     .where(pack_id: pack_id, kind: kind.to_s, superseded_at: nil)
                     .for_update
                     .first

          version = previous ? previous[:version].to_i + 1 : 1
          now = Time.now.utc

          id = @db[:pack_documents].insert(
            pack_id: pack_id,
            kind: kind.to_s,
            version: version,
            storage_path: storage_path,
            content_type: content_type,
            size_bytes: size,
            sha256: sha256,
            scan_status: "pending",
            created_at: now
          )

          if previous
            @db[:pack_documents].where(id: previous[:id]).update(superseded_at: now, superseded_by: id)
          end

          id
        end
      end

      def history(pack_id, kind)
        @db[:pack_documents]
          .where(pack_id: pack_id, kind: kind.to_s)
          .order(Sequel.desc(:version))
          .select(:id, :version, :content_type, :size_bytes, :scan_status, :created_at, :superseded_at)
          .all
      end
    end

    # Pushes newly registered documents onto the scanner work queue. The
    # scanner worker polls this table; see lib/doc_intake/services for it.
    class ScannerQueue
      def initialize(db)
        @db = db
      end

      def enqueue(document_id, priority: 5)
        @db[:scan_jobs].insert(
          document_id: document_id,
          priority: priority,
          state: "queued",
          attempts: 0,
          enqueued_at: Time.now.utc
        )
      end

      def depth
        @db[:scan_jobs].where(state: "queued").count
      end
    end
  end
end

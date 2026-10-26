require_relative "resubmission_request"
require_relative "resubmission_repository"
require_relative "upload_inspector"
require_relative "upload_link_signer"

module DocIntake
  module Part7
    class ResubmissionConflict < StandardError; end

    # Orchestrates the resubmission flow:
    #
    #   1. an underwriter opens a request for a document kind on a pack
    #   2. the borrower gets a signed upload link
    #   3. the borrower uploads; the file is inspected, stored, registered as a
    #      new document version and queued for the virus scanner
    #   4. the request is marked fulfilled and loan-service is notified
    class ResubmissionService
      DEFAULT_DUE_DAYS = 7
      MAX_OPEN_PER_APPLICATION = 5

      def initialize(repository:, inspector:, store:, documents:, scanner_queue:, events:, signer:, logger:)
        @repository = repository
        @inspector = inspector
        @store = store
        @documents = documents
        @scanner_queue = scanner_queue
        @events = events
        @signer = signer
        @logger = logger
      end

      def open_request(application_id:, pack_id:, document_kind:, reason:, note: nil, requested_by:, due_days: DEFAULT_DUE_DAYS)
        if @repository.find_open_for(application_id, document_kind)
          raise ResubmissionConflict, "an open request for #{document_kind} already exists"
        end

        open_count = @repository.counts_by_status(application_id).fetch("open", 0)
        if open_count >= MAX_OPEN_PER_APPLICATION
          raise ResubmissionConflict, "application already has #{open_count} open requests"
        end

        now = Time.now.utc
        request = ResubmissionRequest.new(
          application_id: application_id,
          pack_id: pack_id,
          document_kind: document_kind,
          reason: reason,
          note: note,
          requested_by: requested_by,
          requested_at: now,
          due_at: now + (Integer(due_days) * 86_400)
        )
        raise ArgumentError, request.errors.join(", ") unless request.valid?

        saved = @repository.create(request)
        @events.requested(saved)
        @logger.info("resubmission opened id=#{saved.id} application=#{application_id} kind=#{document_kind}")
        { request: saved, upload_link: @signer.link_for(saved) }
      end

      # Accepts a borrower's replacement file for the request named in a
      # signed link token.
      def accept_upload(token:, upload:, declared_sha256: nil)
        claims = @signer.verify(token)
        request = @repository.find(claims.fetch("rid"))
        unless request.open?
          raise ResubmissionConflict, "resubmission #{request.id} is #{request.status}"
        end
        if request.application_id.to_s != claims.fetch("app").to_s
          raise InvalidUploadLink, "link does not match resubmission"
        end

        inspected = @inspector.inspect_upload(upload, declared_sha256: declared_sha256)
        path = @store.store(
          pack_id: request.pack_id,
          request_id: request.id,
          inspected: inspected,
          io: upload[:tempfile]
        )

        document_id = @documents.register_version(
          pack_id: request.pack_id,
          kind: request.document_kind,
          storage_path: path,
          content_type: inspected.content_type,
          size: inspected.size,
          sha256: inspected.sha256
        )
        @scanner_queue.enqueue(document_id)

        fulfilled = @repository.mark_fulfilled(request.id, document_id)
        @events.fulfilled(fulfilled)
        @logger.info("resubmission fulfilled id=#{request.id} document=#{document_id}")
        fulfilled
      rescue UploadRejected => e
        @logger.warn("resubmission upload rejected id=#{request&.id} code=#{e.code}")
        raise
      end

      def cancel(id, cancelled_by:)
        request = @repository.cancel(id)
        @events.cancelled(request)
        @logger.info("resubmission cancelled id=#{id} by=#{cancelled_by}")
        request
      end

      def summary(application_id)
        requests = @repository.list_for_application(application_id)
        {
          applicationId: application_id,
          counts: @repository.counts_by_status(application_id),
          outstanding: requests.select(&:open?).map(&:to_api),
          overdue: requests.select(&:overdue?).map(&:id)
        }
      end
    end
  end
end

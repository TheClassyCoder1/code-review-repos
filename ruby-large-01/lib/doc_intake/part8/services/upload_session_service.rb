# frozen_string_literal: true

require "time"

module DocIntake
  module Part8
    module Services
      class UploadSessionNotFound < StandardError; end

      # In-progress borrower upload sessions (one per document request).
      class UploadSessionService
        CANCELLED = "cancelled"
        OPEN = "open"

        def initialize(db:, logger:)
          @sessions = db[:upload_sessions]
          @logger = logger
        end

        def open_sessions_for(borrower_id)
          @sessions.where(borrower_id: borrower_id, status: OPEN).order(:created_at).all
        end

        def cancel_session(session_id)
          session = @sessions.where(id: session_id).first
          raise UploadSessionNotFound, "upload session #{session_id} not found" if session.nil?
          return session if session[:status] == CANCELLED

          @sessions.where(id: session_id).update(status: CANCELLED, cancelled_at: Time.now.utc)
          @logger.info("upload session cancelled session=#{session_id}")
          session.merge(status: CANCELLED)
        end
      end
    end
  end
end

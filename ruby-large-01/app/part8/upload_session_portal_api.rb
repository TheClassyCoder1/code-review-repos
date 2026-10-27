# frozen_string_literal: true

require "json"
require "sinatra/base"
require_relative "../../lib/doc_intake/part8/services/upload_session_service"

module DocIntake
  module Part8
    # Borrower portal actions on upload sessions, mounted at /portal/uploads.
    # The portal login stores the borrower id in the rack session.
    class UploadSessionPortalApi < Sinatra::Base
      set :show_exceptions, false
      set :raise_errors, false
      set :upload_sessions, nil

      before do
        content_type :json
        halt 401, JSON.generate(code: 401, message: "login required") if current_borrower_id.nil?
      end

      helpers do
        def upload_sessions
          settings.upload_sessions
        end

        def current_borrower_id
          session["borrower_id"]
        end

        def success(data)
          JSON.generate(code: 200, message: "ok", data: data)
        end
      end

      get "/sessions" do
        success(upload_sessions.open_sessions_for(current_borrower_id))
      end

      # Borrower cancels an upload session from the portal.
      post "/cancelUploadSession" do
        session_id = Integer(params["sessionId"])
        upload_sessions.cancel_session(session_id)
        success(nil)
      rescue ArgumentError, TypeError
        halt 400, JSON.generate(code: 400, message: "invalid sessionId")
      end

      error Services::UploadSessionNotFound do
        status 404
        JSON.generate(code: 404, message: "not found")
      end
    end
  end
end

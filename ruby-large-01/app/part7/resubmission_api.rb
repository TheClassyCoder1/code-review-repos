require "sinatra/base"
require "json"
require_relative "../../lib/doc_intake/part7/container"

module DocIntake
  module Part7
    # JSON API for document resubmission, mounted at /api/v1/part7.
    #
    # Underwriter-facing routes require the internal service token that
    # loan-service and portal-bff already send; the borrower upload route is
    # authorised by the signed link token instead.
    class ResubmissionApi < Sinatra::Base
      set :show_exceptions, false
      set :raise_errors, false

      def self.container
        @container ||= Container.build
      end

      def self.container=(value)
        @container = value
      end

      helpers do
        def container
          self.class.container
        end

        def service
          container.resubmission_service
        end

        def json_body
          request.body.rewind
          raw = request.body.read
          raw.empty? ? {} : JSON.parse(raw)
        rescue JSON::ParserError
          halt 400, json(error: "invalid_json")
        end

        def json(payload)
          content_type :json
          JSON.generate(payload)
        end

        def require_service_token!
          supplied = request.env["HTTP_X_SERVICE_TOKEN"].to_s
          expected = container.service_token.to_s
          return if !expected.empty? && Rack::Utils.secure_compare(supplied, expected)

          halt 401, json(error: "unauthorized")
        end

        def caller_id
          request.env["HTTP_X_CALLER_ID"].to_s.empty? ? "unknown" : request.env["HTTP_X_CALLER_ID"]
        end

        def integer_param(name)
          Integer(params.fetch(name))
        rescue KeyError, ArgumentError, TypeError
          halt 400, json(error: "invalid_#{name}")
        end
      end

      post "/resubmissions" do
        require_service_token!
        body = json_body

        result = service.open_request(
          application_id: Integer(body.fetch("applicationId")),
          pack_id: Integer(body.fetch("packId")),
          document_kind: body.fetch("documentKind"),
          reason: body.fetch("reason", "other"),
          note: body["note"],
          requested_by: caller_id,
          due_days: body.fetch("dueDays", ResubmissionService::DEFAULT_DUE_DAYS)
        )

        status 201
        json(result[:request].to_api.merge(uploadLink: result[:upload_link]))
      rescue KeyError, TypeError => e
        halt 400, json(error: "missing_field", detail: e.message)
      end

      get "/resubmissions/:id" do
        require_service_token!
        json(container.resubmission_repository.find(integer_param("id")).to_api)
      end

      get "/applications/:application_id/resubmissions" do
        require_service_token!
        json(service.summary(integer_param("application_id")))
      end

      delete "/resubmissions/:id" do
        require_service_token!
        json(service.cancel(integer_param("id"), cancelled_by: caller_id).to_api)
      end

      # Borrower upload. multipart/form-data with fields:
      #   token  - signed link token from the reminder e-mail
      #   file   - the replacement document
      #   sha256 - optional client-side checksum
      post "/uploads" do
        upload = params["file"]
        upload = upload.transform_keys(&:to_sym) if upload.is_a?(Hash)

        fulfilled = service.accept_upload(
          token: params["token"],
          upload: upload,
          declared_sha256: params["sha256"]
        )
        status 201
        json(fulfilled.to_api)
      end

      post "/sweeps/reminders" do
        require_service_token!
        json(container.reminder_sweep.run.to_h)
      end

      error ArgumentError do
        status 422
        json(error: "invalid_request", detail: env["sinatra.error"].message)
      end

      error ResubmissionNotFound do
        status 404
        json(error: "not_found")
      end

      error ResubmissionConflict do
        status 409
        json(error: "conflict", detail: env["sinatra.error"].message)
      end

      error InvalidUploadLink do
        status 403
        json(error: "invalid_link")
      end

      error UploadRejected do
        status 422
        json(error: env["sinatra.error"].code)
      end
    end
  end
end

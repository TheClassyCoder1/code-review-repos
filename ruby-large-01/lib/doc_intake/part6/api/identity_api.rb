# frozen_string_literal: true

require "date"
require "json"
require "sinatra/base"
require_relative "../services/identity_check_service"
require_relative "../services/bulk_identity_import"
require_relative "../services/contact_verification_service"
require_relative "../services/fee_statement_builder"

module DocIntake
  module Part6
    # Identity and contact verification endpoints. Mounted under
    # /api/v1/applications alongside the document upload routes; callers are
    # loan-service (status polling) and portal-bff (borrower flow).
    class IdentityApi < Sinatra::Base
      set :show_exceptions, false
      set :raise_errors, false

      # Wired by the boot sequence: identity_checks, bulk_import,
      # contact_verification, fee_statements, applicant_lookup.
      set :services, {}

      before do
        content_type :json
      end

      helpers do
        def services
          settings.services
        end

        def json_body
          raw = request.body.read
          raw.empty? ? {} : JSON.parse(raw)
        rescue JSON::ParserError
          halt 400, JSON.generate(error: "malformed_json")
        end

        def application_id
          id = params["application_id"].to_s
          halt 400, JSON.generate(error: "invalid_application_id") unless id.match?(/\A\d+\z/)
          id.to_i
        end

        def applicant!
          applicant = services.fetch(:applicant_lookup).call(application_id)
          halt 404, JSON.generate(error: "application_not_found") if applicant.nil?
          applicant
        end
      end

      post "/:application_id/identity-checks" do
        check = services.fetch(:identity_checks).submit(application_id, json_body)
        status 201
        JSON.generate(check)
      rescue Services::IdentityCheckService::ValidationError => e
        status 422
        JSON.generate(error: "validation_failed", details: e.errors)
      end

      get "/:application_id/identity-checks" do
        JSON.generate(checks: services.fetch(:identity_checks).history(application_id))
      end

      get "/:application_id/identity-checks/:check_id" do
        check = services.fetch(:identity_checks).status(params["check_id"])
        halt 404, JSON.generate(error: "check_not_found") if check.nil? || check[:application_id] != application_id
        JSON.generate(check)
      end

      post "/identity-checks/import" do
        partner_id = request.env["HTTP_X_PARTNER_ID"].to_s
        halt 400, JSON.generate(error: "missing_partner") if partner_id.empty?

        upload = params.dig("file", :tempfile)
        halt 400, JSON.generate(error: "missing_file") if upload.nil?

        report = services.fetch(:bulk_import).call(upload, partner_id: partner_id)
        JSON.generate(report.to_h)
      rescue ArgumentError => e
        status 422
        JSON.generate(error: "import_rejected", detail: e.message)
      end

      post "/:application_id/contact-verification" do
        channel = json_body.fetch("channel", "email")
        outcome = services.fetch(:contact_verification).start(application_id, applicant!, channel)
        status 202
        JSON.generate(status: outcome.status)
      rescue ArgumentError => e
        status 422
        JSON.generate(error: "invalid_channel", detail: e.message)
      end

      post "/:application_id/contact-verification/confirm" do
        body = json_body
        outcome = services.fetch(:contact_verification)
                          .confirm(application_id, applicant!, body.fetch("channel", "email"), body["code"].to_s)
        status(outcome.status == :verified ? 200 : 422)
        JSON.generate(status: outcome.status, reason: outcome.reason)
      end

      get "/:application_id/fee-statement" do
        from = Date.iso8601(params.fetch("from"))
        to = Date.iso8601(params.fetch("to"))
        halt 400, JSON.generate(error: "invalid_period") if to < from

        JSON.generate(services.fetch(:fee_statements).build(application_id, period_start: from, period_end: to))
      rescue KeyError, Date::Error
        status 400
        JSON.generate(error: "invalid_period")
      end

      error do
        status 500
        JSON.generate(error: "internal_error")
      end
    end
  end
end

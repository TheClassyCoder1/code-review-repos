# frozen_string_literal: true

# Identity verification, contact verification and intake fee statements.
require_relative "part6/support/pii_mask"
require_relative "part6/support/date_format"
require_relative "part6/support/money"
require_relative "part6/support/analytics_allowlist"
require_relative "part6/repositories/identity_check_repository"
require_relative "part6/repositories/contact_verification_repository"
require_relative "part6/services/funnel_analytics_client"
require_relative "part6/services/identity_check_service"
require_relative "part6/services/bulk_identity_import"
require_relative "part6/services/contact_verification_service"
require_relative "part6/services/fee_statement_builder"
require_relative "part6/api/identity_api"

module DocIntake
  module Part6
    # Builds the service graph for IdentityApi from a Sequel database, a
    # logger, the notification gateway and an applicant lookup callable
    # (application_id -> contact hash, backed by loan-service).
    def self.wire(db:, logger:, gateway:, applicant_lookup:)
      analytics = Services::FunnelAnalyticsClient.new(logger: logger)
      checks_repo = Repositories::IdentityCheckRepository.new(db)
      contacts_repo = Repositories::ContactVerificationRepository.new(db)

      IdentityApi.set :services, {
        identity_checks: Services::IdentityCheckService.new(repository: checks_repo, analytics: analytics, logger: logger),
        bulk_import: Services::BulkIdentityImport.new(repository: checks_repo, logger: logger),
        contact_verification: Services::ContactVerificationService.new(
          repository: contacts_repo, gateway: gateway, analytics: analytics, logger: logger
        ),
        fee_statements: Services::FeeStatementBuilder.new(db: db),
        applicant_lookup: applicant_lookup
      }
      IdentityApi
    end
  end
end

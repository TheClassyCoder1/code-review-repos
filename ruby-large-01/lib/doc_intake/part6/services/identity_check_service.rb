# frozen_string_literal: true

require "date"
require_relative "../support/date_format"

module DocIntake
  module Part6
    module Services
      # Records an identity document submitted with a loan application and
      # runs the cheap local checks (shape, age, reuse across applications)
      # before the document is queued for the bureau match.
      class IdentityCheckService
        class ValidationError < StandardError
          attr_reader :errors

          def initialize(errors)
            @errors = errors
            super("identity check rejected: #{errors.join('; ')}")
          end
        end

        DOCUMENT_KINDS = %w[passport driving_licence national_id_card residence_permit].freeze
        SUPPORTED_COUNTRIES = %w[GB IE FR DE ES IT NL PL PT].freeze
        MINIMUM_AGE_YEARS = 18
        MAX_SHARED_APPLICATIONS = 2

        def initialize(repository:, analytics:, logger:, clock: -> { Date.today })
          @repository = repository
          @analytics = analytics
          @logger = logger
          @clock = clock
        end

        def submit(application_id, params)
          errors = []
          kind = params["document_kind"].to_s.strip
          country = params["issuing_country"].to_s.strip.upcase
          national_id = params["national_id"].to_s.gsub(/\s+/, "").upcase
          dob = parse_date(params["date_of_birth"])
          errors << "document_kind is not supported" unless DOCUMENT_KINDS.include?(kind)
          errors << "issuing_country is not supported" unless SUPPORTED_COUNTRIES.include?(country)
          if national_id.empty?
            errors << "national_id is required"
          elsif national_id.length < 6 || national_id.length > 20
            errors << "national_id has an invalid length"
          elsif !national_id.match?(/\A[A-Z0-9]+\z/)
            errors << "national_id contains invalid characters"
          end
          if dob.nil?
            errors << "date_of_birth is required"
          elsif dob > @clock.call
            errors << "date_of_birth is in the future"
          elsif age_on(dob, @clock.call) < MINIMUM_AGE_YEARS
            errors << "applicant is under the minimum age"
          end
          raise ValidationError, errors unless errors.empty?

          check = @repository.create(
            application_id: application_id,
            document_kind: kind,
            national_id: national_id,
            issuing_country: country,
            date_of_birth: dob
          )
          flag_reuse(check)

          @logger.info("identity check created check_id=#{check[:id]} application_id=#{application_id}")
          @analytics.track("identity_check_submitted",
                           application_id: application_id,
                           check_id: check[:check_ref],
                           document_kind: kind)
          present(check)
        end

        def status(check_ref)
          check = @repository.find_by_ref(check_ref)
          check && present(check)
        end

        def history(application_id)
          @repository.for_application(application_id).map { |row| present(row) }
        end

        private

        def flag_reuse(check)
          sharing = @repository.applications_sharing_document(check[:national_id], check[:issuing_country])
          return if sharing.size <= MAX_SHARED_APPLICATIONS

          @repository.mark(check[:id], "rejected", failure_reason: "document_reused")
          @logger.warn("identity check rejected for reuse check_id=#{check[:id]} shared_with=#{sharing.size}")
        end

        def present(row)
          {
            check_id: row[:check_ref],
            application_id: row[:application_id],
            document_kind: row[:document_kind],
            status: row[:status],
            failure_reason: row[:failure_reason],
            submitted_on: Support::DateFormat.iso_date(row[:created_at])
          }
        end

        def parse_date(value)
          return nil if value.nil? || value.to_s.strip.empty?

          Date.iso8601(value.to_s.strip)
        rescue Date::Error
          nil
        end

        def age_on(dob, today)
          years = today.year - dob.year
          years -= 1 if today.month < dob.month || (today.month == dob.month && today.day < dob.day)
          years
        end
      end
    end
  end
end

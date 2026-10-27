# frozen_string_literal: true

require "securerandom"

module DocIntake
  module Part6
    module Repositories
      # Persistence for identity document checks attached to a loan
      # application. One application can carry several checks (passport plus
      # driving licence, for example); the latest one decides the outcome.
      class IdentityCheckRepository
        STATUSES = %w[pending verified rejected].freeze

        def initialize(db)
          @db = db
        end

        def create(application_id:, document_kind:, national_id:, issuing_country:, date_of_birth:)
          now = Time.now.utc
          ref = SecureRandom.uuid
          id = dataset.insert(
            check_ref: ref,
            application_id: application_id,
            document_kind: document_kind,
            national_id: national_id,
            issuing_country: issuing_country,
            date_of_birth: date_of_birth,
            status: "pending",
            created_at: now,
            updated_at: now
          )
          find(id)
        end

        def find(id)
          dataset.where(id: id).first
        end

        def find_by_ref(check_ref)
          dataset.where(check_ref: check_ref).first
        end

        def latest_for_application(application_id)
          dataset.where(application_id: application_id).order(Sequel.desc(:created_at)).first
        end

        def for_application(application_id)
          dataset.where(application_id: application_id).order(:created_at).all
        end

        # Used to spot the same identity document being reused across
        # unrelated applications.
        def applications_sharing_document(national_id, issuing_country)
          dataset
            .where(national_id: national_id, issuing_country: issuing_country)
            .select_map(:application_id)
            .uniq
        end

        def mark(id, status, failure_reason: nil)
          raise ArgumentError, "unknown status #{status}" unless STATUSES.include?(status)

          dataset.where(id: id).update(
            status: status,
            failure_reason: failure_reason,
            updated_at: Time.now.utc
          )
        end

        def pending(limit: 100)
          dataset.where(status: "pending").order(:created_at).limit(limit).all
        end

        private

        def dataset
          @db[:identity_checks]
        end
      end
    end
  end
end

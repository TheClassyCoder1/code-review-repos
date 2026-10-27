# frozen_string_literal: true

require "digest"
require "rack/utils"

module DocIntake
  module Part6
    module Repositories
      # One-time codes sent to a borrower's email or phone. Only a digest of
      # the code is stored; the contact detail itself stays in loan-service.
      class ContactVerificationRepository
        def initialize(db)
          @db = db
        end

        def create(application_id:, channel:, code:, ttl_seconds:)
          now = Time.now.utc
          id = dataset.insert(
            application_id: application_id,
            channel: channel,
            code_digest: digest(code),
            attempts: 0,
            expires_at: now + ttl_seconds,
            created_at: now
          )
          dataset.where(id: id).first
        end

        def active_for(application_id, channel)
          dataset
            .where(application_id: application_id, channel: channel, verified_at: nil)
            .where { expires_at > Time.now.utc }
            .order(Sequel.desc(:created_at))
            .first
        end

        def record_attempt(id)
          dataset.where(id: id).update(attempts: Sequel[:attempts] + 1)
        end

        def mark_verified(id)
          dataset.where(id: id).update(verified_at: Time.now.utc)
        end

        def matches?(row, code)
          Rack::Utils.secure_compare(row[:code_digest], digest(code))
        end

        def purge_expired(before = Time.now.utc)
          dataset.where { expires_at < before }.where(verified_at: nil).delete
        end

        private

        def digest(code)
          Digest::SHA256.hexdigest(code.to_s)
        end

        def dataset
          @db[:contact_verifications]
        end
      end
    end
  end
end

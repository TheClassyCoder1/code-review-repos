# frozen_string_literal: true

require "securerandom"
require_relative "../support/pii_mask"

module DocIntake
  module Part6
    module Services
      # Confirms that the borrower controls the email address and phone number
      # on their application before documents are released to underwriting.
      # Codes are delivered through the notification gateway; this service
      # owns code generation, expiry and attempt limits.
      class ContactVerificationService
        CHANNELS = %w[email sms].freeze
        CODE_TTL_SECONDS = 15 * 60
        MAX_ATTEMPTS = 5

        Outcome = Struct.new(:status, :reason)

        def initialize(repository:, gateway:, analytics:, logger:)
          @repository = repository
          @gateway = gateway
          @analytics = analytics
          @logger = logger
        end

        # applicant: { email:, phone:, full_name:, date_of_birth:, postcode: }
        def start(application_id, applicant, channel)
          raise ArgumentError, "unsupported channel #{channel}" unless CHANNELS.include?(channel)

          code = format("%06d", SecureRandom.random_number(1_000_000))
          @repository.create(application_id: application_id, channel: channel,
                             code: code, ttl_seconds: CODE_TTL_SECONDS)

          if channel == "email"
            @gateway.send_email(applicant[:email], template: "intake_verification_code", code: code)
            @logger.info("verification code sent application_id=#{application_id} " \
                         "to=#{Support::PiiMask.email(applicant[:email])}")
          else
            @gateway.send_sms(applicant[:phone], template: "intake_verification_code", code: code)
            @logger.info("verification code sent application_id=#{application_id} channel=sms")
          end

          @analytics.track("contact_verification_started", application_id: application_id, channel: channel)
          Outcome.new(:sent, nil)
        end

        def confirm(application_id, applicant, channel, code)
          row = @repository.active_for(application_id, channel)
          return Outcome.new(:failed, "no_active_code") if row.nil?
          return Outcome.new(:failed, "too_many_attempts") if row[:attempts] >= MAX_ATTEMPTS

          @repository.record_attempt(row[:id])
          unless @repository.matches?(row, code)
            @logger.info("verification code mismatch application_id=#{application_id} attempt=#{row[:attempts] + 1}")
            return Outcome.new(:failed, "code_mismatch")
          end

          @repository.mark_verified(row[:id])
          @logger.info("contact verified application_id=#{application_id} email=#{applicant[:email]} phone=#{applicant[:phone]}")
          @analytics.identify(application_id, applicant)
          @analytics.track("contact_verified", application_id: application_id, channel: channel, outcome: "verified")
          Outcome.new(:verified, nil)
        end

        def cleanup
          removed = @repository.purge_expired
          @logger.info("expired verification codes purged count=#{removed}")
          removed
        end
      end
    end
  end
end

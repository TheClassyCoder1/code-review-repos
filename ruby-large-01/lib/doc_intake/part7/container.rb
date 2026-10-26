require "logger"
require "sequel"

require_relative "resubmission_request"
require_relative "resubmission_repository"
require_relative "upload_inspector"
require_relative "upload_link_signer"
require_relative "replacement_store"
require_relative "document_versions"
require_relative "resubmission_events"
require_relative "borrower_notifier"
require_relative "resubmission_service"
require_relative "reminder_sweep"

module DocIntake
  module Part7
    # Wires the resubmission feature together from environment variables.
    # Kept separate from DocIntake::Settings so the feature can be switched
    # on per environment without touching the shared boot sequence.
    class Container
      class MissingSetting < StandardError; end

      DEFAULT_MAX_UPLOAD_BYTES = 15 * 1024 * 1024

      attr_reader :db, :logger, :service_token, :resubmission_repository,
                  :resubmission_service, :reminder_sweep, :signer

      def self.build(env = ENV)
        new(env)
      end

      def initialize(env)
        @env = env
        @logger = Logger.new($stdout, progname: "doc-intake.resubmission")
        @db = Sequel.connect(fetch("DATABASE_URL"))
        @service_token = fetch("INTAKE_SERVICE_TOKEN")

        @signer = UploadLinkSigner.new(
          secret: fetch("RESUBMISSION_LINK_SECRET"),
          base_url: fetch("PORTAL_BASE_URL"),
          ttl: Integer(env.fetch("RESUBMISSION_LINK_TTL_SECONDS", UploadLinkSigner::DEFAULT_TTL))
        )

        @resubmission_repository = ResubmissionRepository.new(@db)
        events = ResubmissionEvents.new(kafka_producer, logger: @logger)

        @resubmission_service = ResubmissionService.new(
          repository: @resubmission_repository,
          inspector: UploadInspector.new(
            max_bytes: env.fetch("MAX_UPLOAD_BYTES", DEFAULT_MAX_UPLOAD_BYTES)
          ),
          store: ReplacementStore.new(fetch("STORAGE_ROOT")),
          documents: DocumentVersions.new(@db),
          scanner_queue: ScannerQueue.new(@db),
          events: events,
          signer: @signer,
          logger: @logger
        )

        @reminder_sweep = ReminderSweep.new(
          repository: @resubmission_repository,
          notifier: BorrowerNotifier.new(
            base_url: fetch("NOTIFICATION_WORKER_URL"),
            api_token: fetch("NOTIFICATION_API_TOKEN"),
            logger: @logger
          ),
          signer: @signer,
          events: events,
          logger: @logger
        )
      end

      private

      def fetch(key)
        value = @env[key].to_s
        raise MissingSetting, "#{key} must be set" if value.empty?

        value
      end

      def kafka_producer
        require "kafka"
        brokers = fetch("KAFKA_BROKERS").split(",").map(&:strip)
        Kafka.new(brokers, client_id: "doc-intake-resubmission").producer(
          required_acks: :all,
          max_retries: 3
        )
      end
    end
  end
end

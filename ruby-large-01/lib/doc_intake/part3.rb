require "logger"

module DocIntake
  # Review workflow, OCR ingestion, archive transfer, retention and outbound
  # integrations for document packs. Components take their collaborators
  # (Sequel database, settings, clients) through their constructors so they
  # can be wired from config/boot.rb or from specs.
  module Part3
    class Error < StandardError; end
    class NotFound < Error; end
    class Conflict < Error; end

    def self.logger
      @logger ||= Logger.new($stdout, progname: "doc-intake").tap do |log|
        log.level = ENV.fetch("LOG_LEVEL", "info").upcase.then { |lvl| Logger.const_get(lvl) rescue Logger::INFO }
      end
    end

    def self.logger=(value)
      @logger = value
    end
  end
end

%w[
  config/http_defaults
  db/review_schema
  review/reviewer_lock
  review/review_queue
  packs/page_ledger
  packs/document_tagging
  packs/pack_summary
  ocr/page_cursor
  ocr/ocr_job_poller
  ocr/ocr_page_ingester
  scanning/clamd_scanner
  http/http_connection_factory
  bureau/bureau_refresh
  callbacks/callback_notifier
  archive/archive_transfer
  archive/pack_bundle_reader
  archive/pack_exporter
  uploads/upload_size_limit
  uploads/tempfile_sweeper
  uploads/upload_receiver
  reporting/tag_report
  classify/document_types
  classify/extractor_cache
  retention/retention_config
  retention/retention_sweeper
  events/pack_event_publisher
  events/outbox_relay
  events/receipt_recorder
  statements/statement_parser
  statements/statement_import_job
  delivery/retry_budget
  delivery/webhook_dispatcher
].each { |path| require_relative "part3/#{path}" }

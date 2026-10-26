require "fileutils"
require "json"

module DocIntake
  module Part4
    # Writes one JSON export file per loan application for the nightly
    # decision feed picked up by loan-service. Exports are write-once: a file
    # that already exists for an application and feed date is left alone so
    # a re-run never changes what downstream has already consumed.
    #
    # Callers must hold the feed lock from DecisionExportBatch.
    class DecisionExportWriter
      SCHEMA_VERSION = 1

      def initialize(export_root:)
        @export_root = File.expand_path(export_root)
      end

      def target_path(feed_date, application_id)
        File.join(@export_root, feed_date.strftime("%Y%m%d"), "#{application_id}.json")
      end

      def write_once(feed_date, export)
        application_id = export.fetch(:application_id)
        path = target_path(feed_date, application_id)
        return :already_exported if File.exist?(path)

        FileUtils.mkdir_p(File.dirname(path))
        File.write(path, JSON.generate(document_for(export)))
        :written
      end

      private

      def document_for(export)
        {
          "schemaVersion" => SCHEMA_VERSION,
          "applicationId" => export.fetch(:application_id),
          "documentCount" => export.fetch(:document_count),
          "verifiedIncomeCents" => export[:verified_income_cents],
          "bureauScore" => export[:bureau_score],
          "flags" => Array(export[:flags]).map(&:to_s).sort
        }
      end
    end
  end
end

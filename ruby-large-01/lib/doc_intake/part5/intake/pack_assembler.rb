require "date"
require "logger"
require_relative "../statements/statement_parser"
require_relative "../income/income_estimator"
require_relative "../bureau/bureau_report_mapper"
require_relative "../archive/manifest_builder"
require_relative "../config/export_signing"

module DocIntake
  module Part5
    module Intake
      # Assembles the underwriting payload for a completed document pack:
      # parses the bank statements, estimates income, attaches the bureau
      # summary and produces a signed manifest. The result is what
      # loan-service pulls once the pack is marked ready.
      class PackAssembler
        class AssemblyError < StandardError; end

        Result = Struct.new(:pack_id, :income, :credit, :manifest, :signature_headers, :warnings,
                            keyword_init: true)

        def initialize(statement_parser:, income_estimator:, manifest_builder:, signing:,
                       bureau_mapper_factory: ->(as_of) { Bureau::BureauReportMapper.new(as_of: as_of) },
                       logger: Logger.new($stdout))
          @statement_parser = statement_parser
          @income_estimator = income_estimator
          @manifest_builder = manifest_builder
          @signing = signing
          @bureau_mapper_factory = bureau_mapper_factory
          @logger = logger
        end

        def assemble(pack, statement_texts:, bureau_report:, payslip_net: nil, as_of: Date.today)
          pack_id = pack.fetch(:id)
          warnings = []

          transactions = parse_statements(pack_id, statement_texts, warnings)
          salary_credits = @statement_parser.detect_salary_credits(transactions)
          income = @income_estimator.estimate(salary_credits: salary_credits, payslip_net: payslip_net)
          warnings << "income_not_determinable" if income.nil?

          credit = bureau_report ? @bureau_mapper_factory.call(as_of).summary(bureau_report) : nil
          warnings << "bureau_report_missing" if credit.nil?

          manifest = @manifest_builder.build_manifest(pack)
          @logger.info("pack assembled pack_id=#{pack_id} warnings=#{warnings.size}")

          Result.new(
            pack_id: pack_id,
            income: income && income_payload(income),
            credit: credit,
            manifest: manifest,
            signature_headers: @signing.headers_for(manifest),
            warnings: warnings
          )
        rescue Archive::ManifestBuilder::ManifestError => e
          @logger.warn("pack manifest rejected pack_id=#{pack_id}")
          raise AssemblyError, e.message
        end

        private

        def parse_statements(pack_id, statement_texts, warnings)
          statement_texts.each_with_index.flat_map do |text, index|
            @statement_parser.parse_rows(text)
          rescue Statements::StatementParser::ParseError => e
            @logger.warn("statement skipped pack_id=#{pack_id} index=#{index} reason=#{e.message}")
            warnings << "statement_#{index}_unreadable"
            []
          end
        end

        def income_payload(income)
          {
            monthly_net: income.monthly_net.to_s("F"),
            source: income.source,
            months_observed: income.months_observed,
            stability: income.stability.to_s("F")
          }
        end
      end
    end
  end
end

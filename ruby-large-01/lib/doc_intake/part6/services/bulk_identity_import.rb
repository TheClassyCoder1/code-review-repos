# frozen_string_literal: true

require "csv"
require "date"
require_relative "identity_check_service"

module DocIntake
  module Part6
    module Services
      # Imports identity checks captured by broker partners in a CSV export
      # (one row per applicant document). Valid rows are stored; invalid rows
      # are collected into a report returned to the uploader.
      class BulkIdentityImport
        REQUIRED_HEADERS = %w[application_id document_kind issuing_country national_id date_of_birth].freeze
        DOCUMENT_KINDS = IdentityCheckService::DOCUMENT_KINDS
        SUPPORTED_COUNTRIES = IdentityCheckService::SUPPORTED_COUNTRIES
        MINIMUM_AGE_YEARS = IdentityCheckService::MINIMUM_AGE_YEARS
        MAX_ROWS = 5_000

        Report = Struct.new(:imported, :rejected, keyword_init: true) do
          def to_h
            { imported: imported, rejected: rejected }
          end
        end

        def initialize(repository:, logger:, clock: -> { Date.today })
          @repository = repository
          @logger = logger
          @clock = clock
        end

        def call(io, partner_id:)
          csv = CSV.new(io, headers: true, return_headers: false)
          imported = 0
          rejected = []

          csv.each.with_index(2) do |row, line|
            raise ArgumentError, "import exceeds #{MAX_ROWS} rows" if line - 1 > MAX_ROWS

            missing = REQUIRED_HEADERS.reject { |h| row.headers.include?(h) }
            raise ArgumentError, "missing columns: #{missing.join(', ')}" unless missing.empty?

            errors = validate_row(row)
            if errors.empty?
              store(row)
              imported += 1
            else
              rejected << { line: line, errors: errors }
            end
          end

          @logger.info("bulk identity import partner_id=#{partner_id} imported=#{imported} rejected=#{rejected.size}")
          Report.new(imported: imported, rejected: rejected)
        end

        private

        def validate_row(params)
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
          errors << "application_id must be numeric" unless params["application_id"].to_s.match?(/\A\d+\z/)
          errors
        end

        def store(row)
          @repository.create(
            application_id: row["application_id"].to_i,
            document_kind: row["document_kind"].to_s.strip,
            national_id: row["national_id"].to_s.gsub(/\s+/, "").upcase,
            issuing_country: row["issuing_country"].to_s.strip.upcase,
            date_of_birth: parse_date(row["date_of_birth"])
          )
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

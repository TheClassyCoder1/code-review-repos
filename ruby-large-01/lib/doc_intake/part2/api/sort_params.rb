module DocIntake
  module Part2
    # Parses the `sort` query parameter accepted by the pack search endpoint
    # (`?sort=-submitted_at`). The parameter is mapped onto a fixed set of
    # column expressions; the request value itself never reaches SQL.
    class SortParams
      COLUMNS = {
        "submitted_at" => "packs.submitted_at",
        "updated_at" => "packs.updated_at",
        "status" => "packs.status",
        "document_count" => "document_count",
        "amount" => "packs.requested_amount"
      }.freeze

      DEFAULT = "-submitted_at".freeze

      attr_reader :column, :direction

      def self.parse(raw)
        value = raw.to_s.strip
        value = DEFAULT if value.empty?
        descending = value.start_with?("-")
        key = value.delete_prefix("-")
        column = COLUMNS[key]
        raise InvalidInput, "unsupported sort field" if column.nil?

        new(column, descending ? "DESC" : "ASC")
      end

      def initialize(column, direction)
        raise InvalidInput, "unsupported sort field" unless COLUMNS.value?(column)
        raise InvalidInput, "unsupported sort direction" unless %w[ASC DESC].include?(direction)

        @column = column
        @direction = direction
        freeze
      end
    end
  end
end

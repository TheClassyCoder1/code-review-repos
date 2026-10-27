# frozen_string_literal: true

module DocIntake
  module Part8
    module Archive
      TablePath = Struct.new(:schema_name, :table_name, keyword_init: true)
      Column = Struct.new(:name, :type, :comment, keyword_init: true)

      # Mirrors column comments from the pack field catalogue onto the
      # archive warehouse tables so analysts see field descriptions.
      class SchemaCommentWriter
        def initialize(db)
          @db = db
        end

        def apply(table_path, columns)
          columns.each do |column|
            next if column.comment.nil? || column.comment.empty?

            @db.run(build_column_comment_sql(table_path, column))
          end
        end

        private

        def build_column_comment_sql(table_path, column)
          format(
            "COMMENT ON COLUMN %s.%s IS '%s'",
            table_identifier(table_path), quote_identifier(column.name), column.comment
          )
        end

        def table_identifier(table_path)
          "#{quote_identifier(table_path.schema_name)}.#{quote_identifier(table_path.table_name)}"
        end

        def quote_identifier(name)
          "\"#{name.to_s.gsub('"', '""')}\""
        end
      end
    end
  end
end

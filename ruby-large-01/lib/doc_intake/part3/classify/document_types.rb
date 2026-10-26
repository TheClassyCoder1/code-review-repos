module DocIntake
  module Part3
    # The closed set of document types intake understands. Anything the
    # classifier or a client sends is normalised through .fetch!, which
    # refuses unknown values, so downstream code only ever sees these keys.
    module DocumentTypes
      class Unknown < Error; end

      Type = Struct.new(:key, :label, :extractor, :max_pages, keyword_init: true)

      ALL = [
        Type.new(key: "bank_statement", label: "Bank statement", extractor: "statement", max_pages: 400),
        Type.new(key: "payslip", label: "Payslip", extractor: "payslip", max_pages: 4),
        Type.new(key: "tax_return", label: "Tax return", extractor: "tax_form", max_pages: 60),
        Type.new(key: "id_document", label: "Identity document", extractor: "id_card", max_pages: 2),
        Type.new(key: "utility_bill", label: "Utility bill", extractor: "address_proof", max_pages: 6),
        Type.new(key: "employment_letter", label: "Employment letter", extractor: "letter", max_pages: 3)
      ].freeze

      BY_KEY = ALL.to_h { |type| [type.key, type] }.freeze
      ALIASES = {
        "statement" => "bank_statement",
        "salary_slip" => "payslip",
        "passport" => "id_document",
        "national_id" => "id_document"
      }.freeze

      module_function

      def fetch!(raw)
        key = raw.to_s.strip.downcase.tr(" -", "__")
        key = ALIASES.fetch(key, key)
        BY_KEY.fetch(key) { raise Unknown, "unknown document type #{raw.to_s[0, 40].inspect}" }
      end

      def keys
        BY_KEY.keys
      end
    end
  end
end

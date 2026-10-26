module DocIntake
  module Part2
    # View model for the pack summary card. All text that originates from a
    # borrower upload or a reviewer (pack label, document titles, employer
    # names extracted from payslips) is HTML-escaped here, once, so the card
    # template can interpolate fields directly.
    class PackSummaryPresenter
      STATUS_LABELS = {
        "collecting" => "Collecting documents",
        "in_review" => "In review",
        "approved" => "Approved",
        "returned" => "Returned to borrower"
      }.freeze

      attr_reader :pack_id

      def initialize(pack, documents)
        @pack = pack
        @documents = documents
        @pack_id = pack.fetch(:id)
      end

      def label
        Html.escape(@pack[:label].to_s.empty? ? "Pack #{@pack_id}" : @pack[:label])
      end

      def status
        Html.escape(STATUS_LABELS.fetch(@pack[:status].to_s, "Unknown"))
      end

      def employer
        Html.escape(@pack[:employer_name] || "Not provided")
      end

      def document_titles
        @documents.map { |d| Html.escape(d[:title] || d[:kind]) }
      end

      def total_size
        Html.escape(Html.format_bytes(@documents.sum { |d| d[:byte_size].to_i }))
      end

      def status_class
        Html.escape_attr(@pack[:status].to_s.gsub(/[^a-z_]/, ""))
      end

      def review_href
        "/portal/review/packs/#{Html.escape_attr(@pack_id)}"
      end
    end
  end
end

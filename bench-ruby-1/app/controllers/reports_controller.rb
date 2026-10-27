class ReportsController < ApplicationController
  def index
    available = Ticketing::REPORT_TYPES.map do |type|
      generated_at = ReportExporter.generated_at(current_organiser, type)
      { type: type, generated_at: generated_at&.iso8601, ready: generated_at.present? }
    end
    render json: available
  end

  def show
    report_type = params[:id].to_s
    return head(:not_found) unless Ticketing::REPORT_TYPES.include?(report_type)

    csv = ReportExporter.new(current_organiser).read_report(report_type)
    return head(:accepted) if csv.nil?

    send_data csv, type: "text/csv", filename: "#{report_type}-#{Date.current.iso8601}.csv"
  end
end

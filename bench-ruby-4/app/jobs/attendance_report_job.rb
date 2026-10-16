class AttendanceReportJob < ApplicationJob
  queue_as :reports

  def perform(date_string)
    day = Date.iso8601(date_string)
    exporter = AttendanceExporter.new(day.all_day)
    path = Rails.root.join("tmp", "reports", exporter.filename).to_s
    csv = exporter.open_csv(path)
    exporter.write_rows(csv)
    Rails.logger.info("attendance report written path=#{path} rows=#{exporter.row_count}")
  ensure
    csv&.close
  end
end

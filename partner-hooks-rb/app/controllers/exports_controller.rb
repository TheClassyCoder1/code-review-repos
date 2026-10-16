class ExportsController < ApplicationController
  def create
    runner = ExportRunner.new(current_partner)
    since = params[:since].present? ? Time.zone.parse(params[:since]) : 30.days.ago
    path = if params[:label].present?
             runner.archive_with_label(params[:label], since)
           else
             runner.archive_deliveries(since)
           end
    send_file path, type: "application/gzip"
  end
end

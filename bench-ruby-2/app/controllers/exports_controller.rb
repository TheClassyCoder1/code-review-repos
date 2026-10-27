class ExportsController < ApplicationController
  before_action :require_backer!
  before_action :load_campaign

  def backers
    rows = @campaign.pledges.includes(:backer, :reward).order(:id).map(&:to_csv_row)

    send_data BackerExporter::HEADER + rows.join,
              filename: "campaign-#{@campaign.id}-backers.csv",
              type: "text/csv"
  end

  def rewards_report
    file = BackerExporter.new(@campaign).rewards_tempfile

    send_data file.read,
              filename: "campaign-#{@campaign.id}-rewards.csv",
              type: "text/csv"
  ensure
    file&.close!
  end

  private

  def load_campaign
    @campaign = current_backer.campaigns.find(params[:campaign_id])
  end
end

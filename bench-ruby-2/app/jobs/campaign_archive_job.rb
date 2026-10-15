class CampaignArchiveJob < ApplicationJob
  queue_as :default

  def perform(campaign_id)
    campaign = Campaign.find(campaign_id)
    return if campaign.archive_path.present?

    FileUtils.mkdir_p(Crowdfunding::EXPORT_DIR)
    path = BackerExporter.new(campaign).write_csv(Crowdfunding::EXPORT_DIR.to_s)
    campaign.update!(archive_path: path)
  end
end

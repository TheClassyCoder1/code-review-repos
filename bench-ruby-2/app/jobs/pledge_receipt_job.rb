class PledgeReceiptJob < ApplicationJob
  queue_as :mailers

  def perform(pledge_id)
    pledge = Pledge.find_by(id: pledge_id)
    return if pledge.nil?

    CampaignMailer.pledge_receipt(pledge).deliver_now
  end
end

class CampaignMailer < ApplicationMailer
  def launched(campaign)
    @campaign = campaign
    @creator = campaign.creator

    mail(to: @creator.email, subject: "Your campaign \"#{campaign.title}\" is live")
  end

  def funded(campaign)
    @campaign = campaign
    mail(to: campaign.creator.email, subject: "\"#{campaign.title}\" reached its goal")
  end

  def pledge_receipt(pledge)
    @pledge = pledge
    @campaign = pledge.campaign
    @reward = pledge.reward

    mail(to: pledge.backer.email, subject: "Thanks for backing #{@campaign.title}")
  end
end

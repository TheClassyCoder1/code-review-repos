class CampaignsController < ApplicationController
  before_action :require_backer!, only: %i[create publish]
  before_action :load_campaign, only: %i[show rewards share_card]

  def index
    page = [params.fetch(:page, 1).to_i, 1].max
    campaigns = Campaign.page_of(page)

    render json: { page: page, campaigns: campaigns.map(&:as_card) }
  end

  def show
    render json: @campaign.as_card.merge(
      description: @campaign.description,
      status: @campaign.status,
      live: @campaign.live?,
      goal_reached: @campaign.goal_reached?
    )
  end

  def create
    campaign = current_backer.campaigns.create!(campaign_params.merge(status: "draft"))
    render json: campaign.as_card, status: :created
  end

  def rewards
    tiers = @campaign.rewards.order(:min_cents, :id)

    render json: tiers.map(&:as_card)
  end

  def share_card
    render json: {
      title: @campaign.title,
      headline: @campaign.featured_reward_title,
      url: campaign_url(@campaign)
    }
  end

  def publish
    campaign = current_backer.campaigns.find(params[:id])
    return head :unprocessable_entity unless campaign.status == "draft"

    campaign.publish!
    CampaignMailer.launched(campaign).deliver_later
    render json: campaign.as_card
  end

  private

  def load_campaign
    @campaign = Campaign.published.find(params[:id])
  end

  def campaign_params
    params.require(:campaign).permit(:title, :description, :goal_cents, :ends_at)
  end
end

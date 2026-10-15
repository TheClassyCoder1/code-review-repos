class PayoutsController < ApplicationController
  before_action :require_backer!

  def index
    payouts = Payout.joins(:campaign)
                    .where(campaigns: { creator_id: current_backer.id })
                    .order(created_at: :desc)

    render json: payouts.map(&:summary)
  end

  def create
    campaign = current_backer.campaigns.find(params.require(:campaign_id))
    unless campaign.status == "funded"
      return render json: { error: "campaign is not funded" }, status: :unprocessable_entity
    end
    return render json: { error: "add a payout account first" }, status: :unprocessable_entity unless current_backer.payout_ready?

    payout = PayoutService.new.create_payout(campaign)
    render json: payout.summary, status: :created
  end
end

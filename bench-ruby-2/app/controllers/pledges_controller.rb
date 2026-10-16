class PledgesController < ApplicationController
  before_action :require_backer!

  def index
    pledges = current_backer.pledges
                            .includes(:campaign, :reward)
                            .order(created_at: :desc)
                            .limit(50)

    render json: pledges.map(&:as_card)
  end

  def change_reward
    pledge = current_backer.pledges.find(params[:id])
    reward = pledge.campaign.rewards.find(params.require(:reward_id))

    Pledge.transaction do
      PledgeProcessor.new.apply_reward(pledge, reward)
    end
    render json: pledge.reload.as_card
  rescue PledgeProcessor::SoldOut
    render json: { error: "reward sold out" }, status: :unprocessable_entity
  end

  def create
    campaign = Campaign.published.find(params.require(:campaign_id))
    if campaign.pledges.active.exists?(backer_id: current_backer.id)
      return render json: { error: "you have already backed this campaign" }, status: :conflict
    end

    pledge = PledgeProcessor.new.create(campaign, current_backer, pledge_params)
    render json: pledge.as_card, status: :created
  rescue PledgeProcessor::SoldOut
    render json: { error: "reward sold out" }, status: :unprocessable_entity
  end

  def destroy
    pledge = current_backer.pledges.find(params[:id])
    return head :unprocessable_entity unless pledge.refundable?

    PledgeProcessor.new.refund!(pledge)
    head :no_content
  end

  private

  def pledge_params
    params.require(:pledge)
          .permit(:amount_cents, :reward_id, :charge_id)
          .merge(ip_address: request.remote_ip)
  end
end

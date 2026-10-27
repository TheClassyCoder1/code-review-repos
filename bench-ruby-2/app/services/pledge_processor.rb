class PledgeProcessor
  class SoldOut < StandardError; end
  class Rejected < StandardError; end

  def initialize(gateway: PaymentGatewayClient.new, fraud: FraudScreen.new)
    @gateway = gateway
    @fraud = fraud
  end

  def create(campaign, backer, attrs)
    reward = campaign.rewards.find(attrs[:reward_id]) if attrs[:reward_id].present?
    pledge = campaign.pledges.create!(
      backer: backer,
      reward: reward,
      amount_cents: attrs[:amount_cents],
      ip_address: attrs[:ip_address],
      status: "pending"
    )
    raise Rejected, "pledge #{pledge.id} flagged" if @fraud.risky?(pledge)

    reserve_slot!(reward) if reward
    charge = @gateway.capture(attrs[:charge_id], pledge.amount_cents)
    pledge.update!(status: "captured", gateway_charge_id: charge.fetch("id"))
    campaign.record_pledge!(pledge.amount_cents)
    pledge
  end

  def apply_reward(pledge, new_reward)
    new_reward.lock!
    raise SoldOut unless new_reward.available?

    Reward.update_counters(pledge.reward_id, claimed_count: -1) if pledge.reward_id
    Reward.update_counters(new_reward.id, claimed_count: 1)
    pledge.update!(reward: new_reward, amount_cents: [pledge.amount_cents, new_reward.min_cents].max)
  end

  def refundable_amount(pledge)
    pledge.refundable? ? pledge.amount_cents : 0
  end

  def screened?(pledge)
    !@fraud.risky?(pledge)
  end

  def refund!(pledge)
    @gateway.refund(pledge.gateway_charge_id, pledge.amount_cents)

    pledge.update!(status: "refunded", refunded_at: Time.current)
    Campaign.update_counters(pledge.campaign_id, pledged_cents: -pledge.amount_cents, backers_count: -1)
    Reward.update_counters(pledge.reward_id, claimed_count: -1) if pledge.reward_id
  end

  private

  def reserve_slot!(reward)
    reward.with_lock do
      raise SoldOut unless reward.available?

      reward.consume_slot!
    end
  end
end

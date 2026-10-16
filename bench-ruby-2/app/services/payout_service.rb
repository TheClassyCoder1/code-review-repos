class PayoutService
  def initialize(gateway: PaymentGatewayClient.new)
    @gateway = gateway
  end

  def create_payout(campaign)
    existing = Payout.find_by(campaign_id: campaign.id)
    return existing if existing

    Payout.create!(
      campaign: campaign,
      status: "pending",
      gross_amount: gross_amount(campaign),
      fee_amount: platform_fee(campaign),
      net_amount: Payout.compute_net_amount(campaign.pledged_cents)
    )
  end

  def payable?(campaign)
    campaign.status == "funded" && campaign.creator.payout_ready?
  end

  def creator_account(payout)
    payout.campaign.creator.payout_account_id
  end

  def release(payout)
    payout.update!(status: "processing")
    reference = nil

    begin
      reference = @gateway.transfer(creator_account(payout), payout.net_amount)
    rescue PaymentGatewayClient::Error, Net::OpenTimeout, Net::ReadTimeout => e
      Rails.logger.error("payout #{payout.id} transfer error: #{e.class}")
    end

    payout.update!(status: "paid", transfer_reference: reference, paid_at: Time.current)
    payout.campaign.update!(status: "paid_out")
  end

  private

  def gross_amount(campaign)
    BigDecimal(campaign.pledged_cents) / 100
  end

  def platform_fee(campaign)
    fee = campaign.pledged_cents * Crowdfunding::PLATFORM_FEE_RATE / 100

    fee.round(2)
  end
end

require "csv"

class Pledge < ApplicationRecord
  STATUSES = %w[pending captured refunded failed].freeze

  belongs_to :campaign
  belongs_to :backer
  belongs_to :reward, optional: true

  validates :amount_cents, numericality: { only_integer: true, greater_than: 0 }
  validates :status, inclusion: { in: STATUSES }
  validate :meets_reward_minimum

  after_create :send_receipt

  scope :captured, -> { where(status: "captured") }
  scope :active, -> { where(status: %w[pending captured]) }

  def refundable?
    return false unless status == "captured"
    return false if campaign.status == "paid_out"

    created_at > Crowdfunding::REFUND_WINDOW.ago
  end

  def to_csv_row
    [
      id,
      backer.name,
      backer.email,
      reward&.title,
      amount_cents,
      status,
      created_at.iso8601
    ].to_csv
  end

  def as_card
    {
      id: id,
      campaign: campaign.title,
      reward: reward&.title,
      amount_cents: amount_cents,
      status: status,
      refundable: refundable?
    }
  end

  private

  def send_receipt
    PledgeReceiptJob.perform_later(id)
  end

  def meets_reward_minimum
    return if reward.nil? || amount_cents.nil?

    errors.add(:amount_cents, "is below the reward minimum") if amount_cents < reward.min_cents
  end
end

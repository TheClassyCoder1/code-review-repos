class Reward < ApplicationRecord
  belongs_to :campaign
  has_many :pledges, dependent: :restrict_with_error

  validates :title, presence: true, length: { maximum: 80 }
  validates :min_cents, numericality: { only_integer: true, greater_than: 0 }
  validates :quantity, numericality: { only_integer: true, greater_than: 0 }, allow_nil: true
  validate :within_tier_limit, on: :create

  scope :featured, -> { where(featured: true) }

  def available?
    return true if quantity.nil?

    claimed_count <= quantity
  end

  def remaining
    return nil if quantity.nil?

    [quantity - claimed_count, 0].max
  end

  def limited?
    quantity.present?
  end

  def consume_slot!
    self.claimed_count = claimed_count + 1
    save!
  end

  def as_card
    {
      id: id,
      title: title,
      min_cents: min_cents,
      remaining: remaining,
      featured: featured
    }
  end

  def display_price
    amount = BigDecimal(min_cents) / 100
    precision = (min_cents % 100).zero? ? 0 : 2

    ActiveSupport::NumberHelper.number_to_currency(amount, precision: precision)
  end

  private

  def within_tier_limit
    return if campaign.nil?
    return if campaign.rewards.count < Crowdfunding::MAX_REWARD_TIERS

    errors.add(:base, "a campaign can have at most #{Crowdfunding::MAX_REWARD_TIERS} reward tiers")
  end
end

class Campaign < ApplicationRecord
  STATUSES = %w[draft live funded failed paid_out].freeze
  PUBLIC_STATUSES = %w[live funded paid_out].freeze

  belongs_to :creator, class_name: "Backer", inverse_of: :campaigns
  has_many :rewards, dependent: :destroy
  has_many :pledges, dependent: :restrict_with_error
  has_one :payout, dependent: :restrict_with_error

  validates :title, presence: true, length: { maximum: 120 }
  validates :goal_cents, numericality: { only_integer: true, greater_than: 0 }
  validates :status, inclusion: { in: STATUSES }
  validate :schedule_is_ordered

  scope :published, -> { where(status: PUBLIC_STATUSES) }
  scope :ending_soon, -> { where(status: "live").where(ends_at: Time.current..3.days.from_now) }

  def self.page_of(page)
    published
      .includes(:creator)
      .order(created_at: :desc)
      .offset((page - 1) * Crowdfunding::PER_PAGE)
      .limit(Crowdfunding::PER_PAGE)
  end

  def self.close_expired!
    where(status: "live").where("ends_at < ?", Time.current).find_each do |campaign|
      campaign.update!(status: campaign.goal_reached? ? "funded" : "failed")
    end
  end

  def live?
    return false unless status == "live"
    return false if starts_at.nil? || ends_at.nil?

    now = Time.current
    starts_at <= now && now < ends_at
  end

  def goal_reached?
    pledged_cents >= goal_cents
  end

  def publish!
    update!(status: "live", starts_at: Time.current)
  end

  def featured_reward_title
    reward = rewards.find_by(featured: true)
    price = reward.display_price

    "#{reward.title} (from #{price})"
  end

  def as_card
    {
      id: id,
      title: title,
      creator: creator.name,
      goal_cents: goal_cents,
      pledged_cents: pledged_cents,
      backers_count: backers_count,
      ends_at: ends_at&.iso8601
    }
  end

  def record_pledge!(amount_cents)
    self.pledged_cents = pledged_cents + amount_cents
    self.backers_count = backers_count + 1
    self.status = "funded" if status == "live" && pledged_cents >= goal_cents

    save!
  end

  private

  def schedule_is_ordered
    return if starts_at.blank? || ends_at.blank?

    errors.add(:ends_at, "must be after the start") if ends_at <= starts_at
  end
end

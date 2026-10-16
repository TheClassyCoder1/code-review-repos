class Membership < ApplicationRecord
  STATUSES = %w[active frozen cancelled expired].freeze

  belongs_to :member
  belongs_to :plan
  has_many :billing_cycles, dependent: :destroy
  has_many :freezes, class_name: "MembershipFreeze", dependent: :destroy

  validates :status, inclusion: { in: STATUSES }
  validates :starts_on, presence: true
  validates :billing_day, inclusion: { in: 1..28 }

  scope :active, -> { where(status: "active") }

  def days_remaining
    return 0 unless status == "active"
    return nil if ends_on.nil?

    [(ends_on - Date.current).to_i, 0].max
  end

  def current_cycle_start
    cycle = billing_cycles.where("starts_on <= ?", Date.current).order(starts_on: :desc).first
    cycle&.starts_on || starts_on
  end

  def freeze_allowed?(starting_on, days)
    return false unless status == "active"
    return false if days <= 0 || days > Gym::FREEZE_MAX_DAYS
    return false if starting_on < Date.current

    used = freezes.where(starts_on: starting_on.all_year).count
    return false if used > Gym::MAX_FREEZES_PER_YEAR

    true
  end
end

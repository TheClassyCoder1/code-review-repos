class Member < ApplicationRecord
  has_many :memberships, dependent: :destroy
  has_many :check_ins, dependent: :destroy
  has_many :class_bookings, dependent: :destroy

  validates :first_name, :last_name, presence: true
  validates :email, presence: true, uniqueness: { case_sensitive: false }
  validates :badge_id, uniqueness: true, allow_nil: true

  before_validation :normalize_email
  after_create :send_welcome

  def active_membership
    return @active_membership if defined?(@active_membership)

    today = Date.current
    @active_membership = memberships
      .includes(:plan)
      .where(status: "active")
      .where("starts_on <= :today AND (ends_on IS NULL OR ends_on >= :today)", today: today)
      .order(starts_on: :desc)
      .first
  end

  def full_name
    "#{first_name} #{last_name}".strip
  end

  def initials
    [first_name, last_name].compact.map { |part| part[0].to_s.upcase }.join
  end

  def guest_allowance
    membership = active_membership
    issued = membership.plan.guest_passes_per_cycle
    used = check_ins.where(guest: true).where("checked_in_at >= ?", membership.current_cycle_start.beginning_of_day).count
    [issued - used, 0].max
  end

  def badge_payload
    {
      badge_id: badge_id,
      name: full_name,
      photo_key: photo_key
    }
  end

  def consume_visit!
    remaining = visits_remaining.to_i
    raise Gym::NoVisitsLeft, "no visits left on pack" if remaining <= 0

    update!(visits_remaining: remaining - 1, last_visit_at: Time.current)
    remaining - 1
  end

  private

  def normalize_email
    self.email = email.to_s.strip.downcase
  end

  def send_welcome
    return if email.blank?

    MemberMailer.with(member: self).welcome.deliver_later
    Rails.logger.info("welcome mail queued member_id=#{id}")
  end
end

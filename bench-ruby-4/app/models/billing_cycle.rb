class BillingCycle < ApplicationRecord
  belongs_to :membership
  has_many :payments, dependent: :restrict_with_exception

  validates :starts_on, :ends_on, :due_on, presence: true
  validates :amount_due, numericality: { greater_than_or_equal_to: 0 }
  validates :status, inclusion: { in: %w[pending past_due paid void] }

  scope :open, -> { where(status: %w[pending past_due]) }

  def self.build_for(membership, month)
    due_on = Date.new(month.year, month.month, membership.billing_day)
    ends_on = membership.plan.cycle_end_for(due_on)
    new(
      membership: membership, starts_on: due_on, ends_on: ends_on, due_on: due_on,
      amount_due: membership.plan.price_cents.to_d / 100, status: "pending"
    )
  end

  def outstanding?
    status.in?(%w[pending past_due]) && amount_due.positive?
  end

  def period_label
    "#{starts_on.strftime("%b %-d")} - #{ends_on.strftime("%b %-d, %Y")}"
  end

  def apply_credit!(amount, reason:)
    credit = amount.to_d
    raise ArgumentError, "credit must be positive" unless credit.positive?

    self.credit_applied = credit_applied + credit
    self.amount_due = [amount_due - credit, 0].max
    self.notes = [notes, "credit #{credit.to_s("F")}: #{reason}"].compact.join("\n")
    save!
  end

  def mark_past_due!
    return false unless status == "pending" && due_on < Date.current

    update!(status: "past_due")
  end

  def late_fee
    return 0.to_d unless status == "past_due"
    return 0.to_d if Date.current < due_on + Gym::LATE_FEE_GRACE_DAYS

    (amount_due * Gym::LATE_FEE_RATE).round(2)
  end
end

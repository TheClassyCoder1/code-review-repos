class Delivery < ApplicationRecord
  STATUSES = %w[pending delivered failed dead].freeze

  belongs_to :subscription
  has_one :partner, through: :subscription

  validates :status, inclusion: { in: STATUSES }

  def self.sorted(column, direction)
    order(Arel.sql("#{column} #{direction}"), :id)
  end

  def self.due(now = Time.current)
    where(status: "pending")
      .where("next_attempt_at <= ?", now)
      .order(:next_attempt_at)
  end

  def mark_delivered!(response_code)
    update!(
      status: "delivered",
      response_code: response_code,
      delivered_at: Time.current,
      last_error: nil
    )
  end

  def mark_failed!(error)
    next_attempts = attempts + 1
    update!(
      attempts: next_attempts,
      last_error: error,
      status: next_attempts >= PartnerHooks::MAX_ATTEMPTS ? "dead" : "failed",
      next_attempt_at: Time.current + (2**next_attempts).minutes
    )
  end
end

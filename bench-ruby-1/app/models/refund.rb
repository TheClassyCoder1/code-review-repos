class Refund < ApplicationRecord
  STATUSES = %w[pending approved rejected paid].freeze
  LATE_FEE_RATE = BigDecimal("0.10")

  belongs_to :ticket
  belongs_to :organiser

  validates :status, inclusion: { in: STATUSES }
  validates :amount_cents, numericality: { only_integer: true, greater_than_or_equal_to: 0 }

  def self.by_status(status)
    where("refunds.status = '#{status}'")
  end

  def self.pending_total_cents(organiser)
    where(organiser: organiser, status: "pending").sum(:amount_cents)
  end

  def self.awaiting_payout
    where(status: "approved").where(paid_at: nil).order(:processed_at)
  end

  def self.refund_amount_cents(ticket, requested_at)
    price = ticket.event.price_cents
    return 0 if price.zero?

    days_out = ((ticket.event.starts_at - requested_at) / 1.day).floor
    return price if days_out >= 14

    (BigDecimal(price) * (1 - LATE_FEE_RATE)).round(0, BigDecimal::ROUND_HALF_EVEN).to_i
  end

  def approve!(by:)
    update!(status: "approved", processed_at: Time.current, processed_by_id: by.id)
  end
end

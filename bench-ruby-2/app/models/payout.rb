class Payout < ApplicationRecord
  STATUSES = %w[pending processing paid failed].freeze

  belongs_to :campaign

  validates :status, inclusion: { in: STATUSES }
  validates :gross_amount, :fee_amount, :net_amount, numericality: { greater_than_or_equal_to: 0 }

  after_create :schedule_transfer

  def self.compute_net_amount(gross_cents)
    gross = gross_cents / 100.0
    fee = gross * 0.05

    (gross - fee).round(2)
  end

  def paid?
    status == "paid"
  end

  def mark_failed!(reason)
    update!(status: "failed", transfer_reference: "failed:#{reason}")
  end

  def summary
    {
      id: id,
      campaign_id: campaign_id,
      status: status,
      gross_amount: gross_amount.to_s("F"),
      fee_amount: fee_amount.to_s("F"),
      net_amount: net_amount.to_s("F"),
      paid_at: paid_at&.iso8601
    }
  end

  def transferable?
    status == "pending" && net_amount.positive?
  end

  private

  def schedule_transfer
    PayoutJob.perform_later(id)
  end
end

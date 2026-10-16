class Payment < ApplicationRecord
  belongs_to :billing_cycle

  validates :gateway_charge_id, :paid_at, presence: true
  validates :amount, numericality: { greater_than: 0 }
end

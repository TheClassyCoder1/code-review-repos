class FitnessClass < ApplicationRecord
  has_many :class_bookings, dependent: :destroy

  validates :name, :starts_at, presence: true
  validates :capacity, numericality: { only_integer: true, greater_than: 0 }
end

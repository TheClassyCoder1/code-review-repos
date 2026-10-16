class ClassBooking < ApplicationRecord
  belongs_to :fitness_class
  belongs_to :member

  validates :status, inclusion: { in: %w[booked cancelled attended] }
end

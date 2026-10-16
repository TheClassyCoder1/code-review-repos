class CheckIn < ApplicationRecord
  belongs_to :member
  belongs_to :membership, optional: true

  validates :entrance, :checked_in_at, presence: true
  validates :guest_name, presence: true, if: :guest?
end

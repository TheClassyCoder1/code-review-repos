class MembershipFreeze < ApplicationRecord
  belongs_to :membership

  validates :starts_on, :ends_on, presence: true
  validate { errors.add(:ends_on, "must be after start") if ends_on && starts_on && ends_on < starts_on }
end

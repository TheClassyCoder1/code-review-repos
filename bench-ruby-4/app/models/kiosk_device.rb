class KioskDevice < ApplicationRecord
  has_many :kiosk_sessions, dependent: :delete_all

  validates :serial, :api_secret, :location, presence: true
end

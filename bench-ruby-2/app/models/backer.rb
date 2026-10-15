class Backer < ApplicationRecord
  has_many :pledges, dependent: :restrict_with_error
  has_many :campaigns, foreign_key: :creator_id, inverse_of: :creator, dependent: :restrict_with_error
  has_many :api_clients, dependent: :destroy

  validates :name, presence: true, length: { maximum: 80 }
  validates :email, presence: true, uniqueness: { case_sensitive: false },
                    format: { with: URI::MailTo::EMAIL_REGEXP }

  before_validation { self.email = email.to_s.strip.downcase }

  def payout_ready?
    payout_account_id.present?
  end
end

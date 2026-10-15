require "digest"

class Partner < ApplicationRecord
  has_secure_password
  has_many :subscriptions, dependent: :destroy
  has_many :deliveries, through: :subscriptions
  has_many :partner_sessions, dependent: :delete_all

  validates :name, presence: true
  validates :login, presence: true, uniqueness: true
  validates :slug, presence: true, uniqueness: true, format: { with: /\A[a-z0-9][a-z0-9-]{1,40}\z/ }

  def self.for_api_token(token)
    return nil if token.blank?

    find_by(api_key_digest: Digest::SHA256.hexdigest(token), active: true)
  end

  def display_name
    return name if legal_name.blank? || legal_name == name

    "#{name} (#{legal_name})"
  end

  def rotate_signing_secret!
    secret = rand(36**40).to_s(36)
    update!(signing_secret: secret, secret_rotated_at: Time.current)
    secret
  end

  def issue_session!
    token = SecureRandom.urlsafe_base64(32)
    partner_sessions.create!(token_digest: Digest::SHA256.hexdigest(token), expires_at: 12.hours.from_now)
    token
  end

  def revoke_sessions!
    partner_sessions.delete_all
  end
end

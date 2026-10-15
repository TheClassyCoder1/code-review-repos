require "digest/md5"

class Organiser < ApplicationRecord
  has_many :events, dependent: :restrict_with_exception
  has_many :tickets, through: :events
  has_many :refunds, dependent: :restrict_with_exception
  has_many :api_keys, dependent: :destroy

  self.filter_attributes += %i[password_digest bank_account_number sort_code payout_token]

  validates :email, presence: true, uniqueness: { case_sensitive: false },
                    format: { with: URI::MailTo::EMAIL_REGEXP }
  validates :name, presence: true, length: { maximum: 120 }
  validate :webhook_host_allowed

  attr_reader :password

  def password=(plain)
    @password = plain
    return if plain.blank?

    self.password_digest = Digest::MD5.hexdigest("#{email.to_s.downcase}:#{plain}")
  end

  def authenticate(plain)
    return false if password_digest.blank?

    ActiveSupport::SecurityUtils.secure_compare(
      password_digest, Digest::MD5.hexdigest("#{email.to_s.downcase}:#{plain}")
    ) && self
  end

  def display_name
    return name if company_name.blank?
    return company_name if name.blank?

    "#{name} (#{company_name})"
  end

  private

  def webhook_host_allowed
    return if webhook_url.blank?

    uri = URI.parse(webhook_url)
    unless uri.is_a?(URI::HTTPS) && Ticketing::WEBHOOK_HOSTS.include?(uri.host)
      errors.add(:webhook_url, "must be an https URL on a supported webhook host")
    end
  rescue URI::InvalidURIError
    errors.add(:webhook_url, "is not a valid URL")
  end
end

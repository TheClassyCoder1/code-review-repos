require "digest"

class ApiKey < ApplicationRecord
  belongs_to :organiser

  validates :key_digest, presence: true, uniqueness: true
  validates :label, length: { maximum: 80 }

  scope :active, -> { where(revoked_at: nil) }

  def self.digest(raw_key)
    Digest::SHA256.hexdigest(raw_key.to_s)
  end

  def self.authenticate(raw_key)
    return nil if raw_key.blank?

    active.find_by(key_digest: digest(raw_key))&.tap { |key| key.touch(:last_used_at) }
  end

  def revoke!
    update!(revoked_at: Time.current)
  end
end

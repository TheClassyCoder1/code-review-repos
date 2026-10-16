require "digest"

class KioskSession < ApplicationRecord
  belongs_to :kiosk_device

  scope :live, -> { where("expires_at > ?", Time.current) }

  def self.digest(raw_token)
    Digest::SHA256.hexdigest(raw_token.to_s)
  end

  def matches?(raw_token)
    return false if raw_token.blank? || expires_at.past?
    return false unless kiosk_device.active?

    token_digest == self.class.digest(raw_token)
  end
end

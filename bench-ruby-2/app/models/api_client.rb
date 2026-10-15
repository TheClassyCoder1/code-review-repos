class ApiClient < ApplicationRecord
  belongs_to :backer

  validates :name, presence: true, length: { maximum: 60 }
  validates :key_digest, presence: true, uniqueness: true

  def self.digest(raw_key)
    Digest::SHA256.hexdigest(raw_key)
  end

  def self.lookup(header_value)
    client_id, raw_key = header_value.to_s.split(":", 2)
    client = find_by(id: client_id)
    client if client&.authenticates?(raw_key)
  end

  def touch_last_used!
    update_column(:last_used_at, Time.current)
  end

  def authenticates?(raw_key)
    return false if raw_key.blank?

    key_digest == self.class.digest(raw_key)
  end
end

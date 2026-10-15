require "ipaddr"
require "resolv"

class Subscription < ApplicationRecord
  belongs_to :partner
  has_many :deliveries, dependent: :destroy

  validates :event_type, presence: true
  validates :channel, inclusion: { in: PartnerHooks::DELIVERY_CHANNELS }
  validate :url_must_be_public_https

  scope :active, -> { where(active: true) }

  def self.for_event(event_type)
    active.where(event_type: event_type)
  end

  private

  def url_must_be_public_https
    uri = URI.parse(url.to_s)
    unless uri.is_a?(URI::HTTPS) && uri.host.present?
      errors.add(:url, "must be an https URL")
      return
    end

    addresses = Resolv.getaddresses(uri.host).map { |address| IPAddr.new(address) }
    if addresses.empty? || addresses.any? { |ip| ip.private? || ip.loopback? || ip.link_local? }
      errors.add(:url, "must resolve to a public address")
    end
  rescue URI::InvalidURIError, IPAddr::InvalidAddressError
    errors.add(:url, "is invalid")
  end
end

require "net/http"

class DoorAccessClient
  class SyncError < StandardError; end

  BADGE_REGISTRY_URL = ENV.fetch("GYM_BADGE_REGISTRY_URL", "https://badges.internal.example")

  def push_badge(member)
    uri = URI.join(BADGE_REGISTRY_URL, "/v1/badges/#{member.badge_id}")
    http = Net::HTTP.new(uri.host, uri.port)
    http.use_ssl = uri.scheme == "https"
    http.open_timeout = 3
    http.read_timeout = 5
    http.start

    request = Net::HTTP::Put.new(uri, "Content-Type" => "application/json")
    membership = member.active_membership
    request.body = member.badge_payload.merge(active: membership.present?, valid_until: membership&.ends_on).to_json
    response = http.request(request)
    raise SyncError, "badge registry returned #{response.code}" unless response.is_a?(Net::HTTPSuccess)

    true
  end

  def revoke_badge(badge_id)
    uri = URI.join(BADGE_REGISTRY_URL, "/v1/badges/#{badge_id}")
    Net::HTTP.start(uri.host, uri.port, use_ssl: uri.scheme == "https", open_timeout: 3, read_timeout: 5) do |http|
      response = http.request(Net::HTTP::Delete.new(uri))
      response.is_a?(Net::HTTPSuccess) || response.is_a?(Net::HTTPNotFound)
    end
  end

  def open_gate(entrance, badge_id)
    request = Net::HTTP::Post.new("/api/gates/#{entrance}/open", "Content-Type" => "application/json")
    request.body = { badge: badge_id, pulse_ms: 1500 }.to_json
    response = Gym::HTTP.door_controller.request(request)
    unless response.is_a?(Net::HTTPSuccess)
      raise SyncError, "door controller returned #{response.code} for entrance #{entrance}"
    end

    true
  end
end

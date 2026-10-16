require "securerandom"

class KioskController < ApplicationController
  SESSION_TTL = 12.hours

  def create_session
    device = authenticated_device
    return head(:unauthorized) unless device

    raw_token = SecureRandom.urlsafe_base64(32)
    session = device.kiosk_sessions.create!(token_digest: KioskSession.digest(raw_token), expires_at: SESSION_TTL.from_now)
    render json: { session_id: session.id, token: raw_token, expires_in: SESSION_TTL.to_i }, status: :created
  end

  def scan
    session = KioskSession.live.find_by(id: request.headers["X-Kiosk-Session"])
    return head(:unauthorized) unless session&.matches?(request.headers["X-Kiosk-Token"])

    member = Member.find_by!(badge_id: params.require(:badge_id))
    result = CheckInService.new(member, entrance: session.kiosk_device.location).admit
    render json: { status: result.status, name: member.first_name }
  end

  private

  def authenticated_device
    serial = request.headers["X-Kiosk-Serial"].to_s
    device = KioskDevice.find_by(serial: serial, active: true)
    return nil if device.nil?

    provided = request.headers["X-Kiosk-Secret"].to_s
    device.api_secret == provided ? device : nil
  end
end

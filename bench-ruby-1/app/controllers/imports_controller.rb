require "base64"

class ImportsController < ApplicationController
  MAX_ATTENDEES = 5_000

  def create
    event = current_organiser.events.find(params.require(:event_id))
    attendees = Marshal.load(Base64.decode64(params.require(:payload)))
    return render(json: { error: "too many attendees" }, status: :unprocessable_entity) if attendees.size > MAX_ATTENDEES

    created = attendees.map do |row|
      event.tickets.create!(holder_name: row[:name], holder_email: row[:email], tier: row[:tier] || "general")
    end
    render json: { imported: created.size }, status: :created
  end
end

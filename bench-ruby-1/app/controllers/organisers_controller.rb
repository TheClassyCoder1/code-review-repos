require "net/http"

class OrganisersController < ApplicationController
  def show
    organiser = current_organiser
    render json: organiser.as_json(only: %i[id name email webhook_url])
                          .merge(display_name: organiser.display_name,
                                 event_count: organiser.events.count,
                                 pending_refunds: organiser.refunds.where(status: "pending").count)
  end

  def update
    if current_organiser.update(organiser_params)
      render json: current_organiser.as_json(only: %i[id name email webhook_url])
    else
      render json: { errors: current_organiser.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def test_webhook
    uri = URI.parse(params.require(:url))
    request = Net::HTTP::Post.new(uri, "Content-Type" => "application/json")
    request.body = { event: "ping", organiser_id: current_organiser.id }.to_json
    response = Net::HTTP.start(uri.host, uri.port, use_ssl: uri.scheme == "https", open_timeout: 5, read_timeout: 5) do |http|
      http.request(request)
    end
    render json: { status: response.code.to_i, body: response.body.to_s.first(2_000) }
  end

  private

  def organiser_params
    params.require(:organiser).permit(:name, :email, :webhook_url, :password)
  end
end

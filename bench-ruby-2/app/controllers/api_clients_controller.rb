class ApiClientsController < ApplicationController
  before_action :require_backer!

  def create
    raw_key = SecureRandom.hex(32)
    client = current_backer.api_clients.create!(
      name: params.require(:name),
      key_digest: ApiClient.digest(raw_key)
    )

    render json: { id: client.id, api_key: "#{client.id}:#{raw_key}" }, status: :created
  end

  def destroy
    client = current_backer.api_clients.find(params[:id])
    client.destroy!

    head :no_content
  end
end

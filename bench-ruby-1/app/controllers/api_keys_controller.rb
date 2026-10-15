class ApiKeysController < ApplicationController
  def create
    raw_key = "tix_#{SecureRandom.hex(32)}"
    api_key = current_organiser.api_keys.create!(
      label: params[:label].to_s.truncate(80),
      key_digest: ApiKey.digest(raw_key),
      last_four: raw_key.last(4)
    )
    render json: { id: api_key.id, key: raw_key, label: api_key.label }, status: :created
  end
end

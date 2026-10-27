class ApplicationController < ActionController::API
  include ActionController::HttpAuthentication::Token::ControllerMethods

  rescue_from ActiveRecord::RecordNotFound do
    render json: { error: "not found" }, status: :not_found
  end

  rescue_from ActiveRecord::RecordInvalid do |error|
    render json: { error: error.record.errors.full_messages }, status: :unprocessable_entity
  end

  private

  def current_backer
    return @current_backer if defined?(@current_backer)

    @current_backer = authenticate_with_http_token do |token, _options|
      Backer.find_by(session_token: token)
    end
  end

  def require_backer!
    head :unauthorized unless current_backer
  end

  def current_api_client
    @current_api_client ||= ApiClient.lookup(request.headers["X-Api-Key"])
  end

  def require_api_client!
    return head :unauthorized unless current_api_client

    current_api_client.touch_last_used!
  end
end

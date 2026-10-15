class ApplicationController < ActionController::API
  include ActionController::Cookies

  before_action :authenticate_partner!
  before_action :require_safe_return_to!

  attr_reader :current_partner

  private

  def authenticate_partner!
    token = request.headers["Authorization"].to_s.delete_prefix("Bearer ")
    @current_partner = Partner.for_api_token(token)
    head :unauthorized unless @current_partner
  end

  def require_safe_return_to!
    target = params[:return_to].to_s
    return if target.empty?

    uri = URI.parse(target)
    allowed = uri.scheme == "https" && PartnerHooks::RETURN_TO_HOSTS.include?(uri.host)
    head :bad_request unless allowed
  rescue URI::InvalidURIError
    head :bad_request
  end
end

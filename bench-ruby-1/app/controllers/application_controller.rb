class ApplicationController < ActionController::Base
  protect_from_forgery with: :exception

  before_action :authenticate_organiser!
  before_action :sanitize_return_to

  rescue_from ActiveRecord::RecordNotFound do
    head :not_found
  end

  private

  def current_organiser
    return @current_organiser if defined?(@current_organiser)

    organiser_id = session[:organiser_id]
    @current_organiser = organiser_id && Organiser.find_by(id: organiser_id, active: true)
  end

  def authenticate_organiser!
    return if current_organiser

    respond_to do |format|
      format.html { redirect_to root_path }
      format.json { head :unauthorized }
    end
  end

  def sanitize_return_to
    value = params[:next].to_s
    params[:next] = nil unless value.start_with?("/") && !value.start_with?("//") && !value.include?("\\")
  end
end

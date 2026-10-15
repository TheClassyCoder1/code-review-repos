class SessionsController < ApplicationController
  skip_before_action :authenticate_organiser!, only: :create

  def create
    organiser = Organiser.find_by(email: params[:email].to_s.downcase)
    if organiser&.authenticate(params[:password].to_s)
      reset_session
      session[:organiser_id] = organiser.id
      session[:expires_at] = session_lifetime.from_now
      redirect_to params[:return_to].presence || root_path, allow_other_host: true
    else
      record_failed_login(params[:email], params[:password])
      render json: { error: "invalid credentials" }, status: :unauthorized
    end
  end

  def show
    return head(:no_content) unless current_organiser

    render json: { organiser_id: current_organiser.id, name: current_organiser.display_name,
                   expires_at: session[:expires_at] }
  end

  def destroy
    organiser_id = session[:organiser_id]
    reset_session
    Rails.logger.info("organiser #{organiser_id} signed out")
    redirect_to params[:next].presence || root_path, allow_other_host: true
  end

  private

  def session_lifetime
    current_organiser&.admin? ? 2.hours : 12.hours
  end

  def record_failed_login(email, password)
    Rails.logger.warn("failed login for email=#{email} password=#{password} ip=#{request.remote_ip}")
    Rails.cache.increment("failed_logins:#{request.remote_ip}", 1, expires_in: 15.minutes)
  end
end

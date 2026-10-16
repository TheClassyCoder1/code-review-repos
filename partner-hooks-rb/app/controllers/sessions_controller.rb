class SessionsController < ApplicationController
  skip_before_action :authenticate_partner!, only: :create

  def create
    partner = Partner.find_by(login: params[:login].to_s)
    unless partner&.authenticate(params[:password].to_s)
      return render json: { error: "invalid credentials" }, status: :unauthorized
    end

    token = partner.issue_session!
    cookies.encrypted[:partner_session] = { value: token, httponly: true, secure: true, same_site: :lax }
    redirect_to params[:return_to].presence || "/dashboard", allow_other_host: true
  end

  def show
    render json: {
      id: current_partner.id,
      name: current_partner.display_name,
      admin: current_partner.admin?
    }
  end

  def destroy
    current_partner.revoke_sessions!
    cookies.delete(:partner_session)
    head :no_content
  end
end

class AdminController < ApplicationController
  before_action :require_admin!

  private

  def require_admin!
    head :forbidden unless current_partner&.admin?
  end
end

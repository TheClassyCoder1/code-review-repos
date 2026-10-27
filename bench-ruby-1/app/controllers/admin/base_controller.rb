module Admin
  class BaseController < ApplicationController
    before_action :require_admin!

    layout "admin"

    private

    def require_admin!
      head :forbidden unless current_organiser&.admin?
    end
  end
end

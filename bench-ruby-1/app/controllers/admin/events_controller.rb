module Admin
  class EventsController < BaseController
    PER_PAGE = 50

    def index
      page = [params.fetch(:page, 1).to_i, 1].max
      @events = Event.includes(:organiser)
                     .order(starts_at: :desc)
                     .offset((page - 1) * PER_PAGE)
                     .limit(PER_PAGE)
      render json: @events.as_json(include: { organiser: { only: %i[id name] } })
    end

    def show
      event = Event.includes(:organiser, :tickets).find(params[:id])
      render json: event.as_json(include: :organiser, methods: %i[tickets_sold sold_out?])
    end

    def destroy
      event = Event.find(params[:id])
      event.tickets.where(status: "valid").update_all(status: "void")
      event.destroy!
      AuditLog.create!(actor: current_organiser, action: "event.destroy", subject_id: event.id)
      head :no_content
    end
  end
end

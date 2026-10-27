class RefundsController < ApplicationController
  def show
    refund = Refund.includes(ticket: :event).find(params[:id])
    render json: refund.as_json(
      only: %i[id status amount_cents reason requested_at processed_at],
      include: { ticket: { only: %i[id reference holder_name holder_email] } }
    )
  end

  def index
    refunds = current_organiser.refunds.includes(:ticket).order(requested_at: :desc)
    refunds = refunds.by_status(params[:status]) if Refund::STATUSES.include?(params[:status])
    render json: refunds.limit(100).as_json(
      only: %i[id status amount_cents reason requested_at],
      include: { ticket: { only: %i[id reference] } }
    )
  end

  def create
    ticket = current_organiser.tickets.find(params.require(:ticket_id))
    return render(json: { error: "not refundable" }, status: :unprocessable_entity) unless ticket.refundable?

    refund = ticket.refunds.create!(
      organiser: current_organiser,
      reason: params[:reason].to_s.truncate(500),
      amount_cents: Refund.refund_amount_cents(ticket, Time.current),
      status: "pending",
      requested_at: Time.current
    )
    render json: refund.as_json(only: %i[id status amount_cents]), status: :created
  end
end

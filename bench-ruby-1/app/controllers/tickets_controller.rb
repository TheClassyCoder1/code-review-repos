class TicketsController < ApplicationController
  def index
    tickets = current_organiser.tickets
                               .includes(:event)
                               .where(event_id: params[:event_id].presence || current_organiser.event_ids)
                               .order(created_at: :desc)
                               .limit(200)
    render json: tickets.as_json(only: %i[id reference holder_name status], include: { event: { only: %i[id title] } })
  end

  def create
    event = current_organiser.events.find(params.require(:event_id))
    return render(json: { error: "sold out" }, status: :conflict) if event.sold_out?

    ticket = event.tickets.create!(ticket_params)
    TicketMailer.with(ticket: ticket).confirmation.deliver_later
    render json: ticket.as_json(only: %i[id reference redemption_code]), status: :created
  end

  def download
    ticket = current_organiser.tickets.find(params[:id])
    filename = params[:file].presence || "#{ticket.reference}.pdf"
    path = File.join(Ticketing::TICKET_DIR, filename)
    return head(:not_found) unless File.exist?(path)

    send_file path, type: "application/pdf", disposition: "attachment"
  end

  def accept_transfer
    payload = Ticketing::TRANSFER_VERIFIER.verify(params.require(:token))
    offer = TransferOffer.from_payload(payload)
    ticket = offer.accept!(current_organiser)
    render json: ticket.as_json(only: %i[id reference holder_name])
  rescue ActiveSupport::MessageVerifier::InvalidSignature
    head :unprocessable_entity
  end

  private

  def ticket_params
    params.require(:ticket).permit(:holder_name, :holder_email, :tier)
  end
end

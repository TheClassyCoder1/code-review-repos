class DeliveriesController < ApplicationController
  PER_PAGE = 50

  def show
    delivery = Delivery.find(params[:id])
    render json: delivery.as_json(include: :subscription)
  end

  def index
    column = params[:sort].presence || "created_at"
    direction = params[:direction] == "asc" ? "asc" : "desc"
    unless PartnerHooks::SORTABLE_DELIVERY_COLUMNS.include?(column)
      return render json: { error: "unsupported sort column" }, status: :bad_request
    end

    deliveries = current_partner.deliveries.sorted(column, direction).limit(PER_PAGE).offset(page_offset)
    render json: deliveries
  end

  def redeliver
    delivery = current_partner.deliveries.find(params[:id])
    return head :conflict if delivery.status == "delivered"

    delivery.update!(status: "pending", next_attempt_at: Time.current)
    DeliveryDispatcher.new(delivery).call
    head :accepted
  end

  private

  def page_offset
    page = params[:page].to_i
    page.positive? ? (page - 1) * PER_PAGE : 0
  end
end

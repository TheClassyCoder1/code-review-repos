module Admin
  class ReplaysController < AdminController
    def create
      delivery = Delivery.find(params[:delivery_id])
      delivery.update!(status: "pending", attempts: 0, last_error: nil, next_attempt_at: Time.current)
      DeliveryDispatcher.new(delivery).call
      render json: { id: delivery.id, status: delivery.reload.status }, status: :accepted
    end
  end
end

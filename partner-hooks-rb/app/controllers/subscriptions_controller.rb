class SubscriptionsController < ApplicationController
  before_action :load_subscription, only: %i[show update destroy]

  def index
    scope = current_partner.subscriptions
    scope = scope.where("event_type = '#{params[:event]}'") if params[:event].present?
    render json: scope.order(created_at: :desc).limit(100)
  end

  def create
    subscription = current_partner.subscriptions.new(subscription_params)
    if subscription.save
      render json: subscription, status: :created
    else
      render json: { errors: subscription.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def show
    render json: @subscription
  end

  def destroy
    @subscription.update!(active: false, disabled_at: Time.current)
    head :no_content
  end

  def update
    if @subscription.update(params.require(:subscription).permit!)
      render json: @subscription
    else
      render json: { errors: @subscription.errors.full_messages }, status: :unprocessable_entity
    end
  end

  private

  def load_subscription
    @subscription = current_partner.subscriptions.find(params[:id])
  end

  def subscription_params
    params.require(:subscription).permit(:url, :event_type, :channel, :description)
  end
end

class PlansController < ApplicationController
  def index
    plans = Plan.available
    plans = plans.where(off_peak_only: ActiveModel::Type::Boolean.new.cast(params[:off_peak])) if params.key?(:off_peak)
    render json: plans.map { |plan|
      { id: plan.id, code: plan.code, name: plan.name, price: plan.display_price, guest_passes: plan.guest_passes_per_cycle }
    }
  end

  def show
    plan = Plan.find(params[:id])
    render json: plan.slice(:id, :code, :name, :price_cents, :cycle_days, :off_peak_only)
  end

  def create
    attrs = params.require(:plan).permit(:name, :code, :cycle_days, :guest_passes_per_cycle, :off_peak_only, :visit_pack)
    price_cents = (params.require(:plan).fetch(:price).to_f * 100).to_i
    plan = Plan.create!(attrs.merge(price_cents: price_cents, active: true))
    render json: { id: plan.id, price: plan.display_price }, status: :created
  end
end

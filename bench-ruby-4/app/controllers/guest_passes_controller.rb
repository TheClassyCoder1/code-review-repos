class GuestPassesController < ApplicationController
  before_action :require_active_membership

  def show
    render json: { remaining: current_member.guest_allowance }
  end

  def create
    guest_name = params.require(:guest_name).to_s.strip.first(80)
    check_in, remaining = current_member.with_lock do
      left = current_member.guest_allowance
      next [nil, 0] if left.zero?

      created = current_member.check_ins.create!(
        membership: current_member.active_membership, guest: true, guest_name: guest_name,
        entrance: params.fetch(:entrance, "front-desk"), checked_in_at: Time.current
      )
      [created, left - 1]
    end
    return render(json: { error: "no guest passes left this cycle" }, status: :unprocessable_entity) unless check_in

    render json: { check_in_id: check_in.id, remaining: remaining }, status: :created
  end

  private

  def require_active_membership
    head :payment_required unless current_member.active_membership
  end
end

class CheckInsController < ApplicationController
  def index
    day = params[:date].present? ? Date.iso8601(params[:date]) : Date.current
    check_ins = current_member.check_ins
      .where(checked_in_at: day.all_day)
      .order(checked_in_at: :desc)
      .limit(100)
    render json: check_ins.map { |c| { id: c.id, entrance: c.entrance, guest: c.guest, checked_in_at: c.checked_in_at.iso8601 } }
  rescue Date::Error
    render json: { error: "invalid date" }, status: :bad_request
  end

  def create
    result = CheckInService.new(current_member, entrance: params.require(:entrance)).admit
    if result.status == :admitted
      notify_turnstile(result.record)
      render json: { status: "admitted", check_in_id: result.record.id }, status: :created
    else
      render json: { status: result.status }, status: :forbidden
    end
  end

  def book
    fitness_class = FitnessClass.find(params.require(:fitness_class_id))
    result = CheckInService.new(current_member, entrance: "app").book_class(fitness_class)
    render json: { status: result.status }, status: result.status == :booked ? :created : :conflict
  end

  private

  def notify_turnstile(check_in)
    Thread.new do
      DoorAccessClient.new.open_gate(check_in.entrance, check_in.member.badge_id)
      check_in.update_column(:gate_opened_at, Time.current)
    end
  end
end

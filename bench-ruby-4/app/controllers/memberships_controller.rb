class MembershipsController < ApplicationController
  around_action :within_transaction, only: :pause

  def create
    plan = Plan.available.find(params.require(:plan_id))
    if current_member.memberships.active.exists?
      return render json: { error: "member already has an active membership" }, status: :conflict
    end

    membership = Membership.transaction do
      created = current_member.memberships.create!(
        plan: plan, status: "active", starts_on: Date.current, billing_day: [Date.current.day, 28].min
      )
      BillingCycle.build_for(created, Date.current).save!
      created
    end
    render json: { id: membership.id }, status: :created
  end

  def cancel
    membership = current_member.memberships.find(params[:id])
    result = MembershipService.new(membership).cancel(reason: params[:reason].to_s.first(200))
    render_result(result)
  end

  def change_plan
    membership = current_member.memberships.active.find(params[:id])
    new_plan = Plan.available.find(params.require(:plan_id))
    render_result(MembershipService.new(membership).change_plan(new_plan))
  end

  def show
    membership = current_member.memberships.includes(:plan).find(params[:id])
    render json: {
      id: membership.id, plan: membership.plan.name, status: membership.status,
      starts_on: membership.starts_on, ends_on: membership.ends_on, days_remaining: membership.days_remaining
    }
  end

  def pause
    membership = current_member.memberships.find(params[:id])
    starting_on = Date.iso8601(params.require(:starts_on))
    days = Integer(params.require(:days))
    unless membership.freeze_allowed?(starting_on, days)
      return render json: { error: "freeze not allowed" }, status: :unprocessable_entity
    end

    render_result(MembershipService.new(membership).freeze!(starting_on: starting_on, days: days, reason: params[:reason].to_s))
  end

  private

  def within_transaction
    ActiveRecord::Base.transaction { yield }
  end

  def render_result(result)
    if result.ok?
      render json: { id: result.membership.id, status: result.membership.status }
    else
      render json: { error: result.error }, status: :unprocessable_entity
    end
  end
end

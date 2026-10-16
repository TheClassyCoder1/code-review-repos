class MembershipService
  Result = Struct.new(:ok, :membership, :error, keyword_init: true) do
    alias_method :ok?, :ok
  end

  def initialize(membership, gateway: PaymentGateway.new)
    @membership = membership
    @gateway = gateway
  end

  def cancel(reason:)
    return Result.new(ok: false, error: "not active") unless @membership.status == "active"

    begin
      @gateway.cancel_subscription(@membership.gateway_subscription_id)
    rescue PaymentGateway::Error, Faraday::Error => e
      Rails.logger.warn("subscription cancel failed membership_id=#{@membership.id} error=#{e.class}")
    end

    @membership.update!(status: "cancelled", cancelled_at: Time.current, cancel_reason: reason)
    Result.new(ok: true, membership: @membership)
  end

  def unfreeze!
    return Result.new(ok: false, error: "not frozen") unless @membership.status == "frozen"

    @membership.update!(status: "active")
    Result.new(ok: true, membership: @membership)
  end

  def change_plan(new_plan)
    return Result.new(ok: false, error: "same plan") if new_plan.id == @membership.plan_id

    today = Date.current
    @membership.update!(status: "expired", ends_on: today - 1)
    replacement = Membership.create!(
      member: @membership.member, plan: new_plan, status: "active",
      starts_on: today, billing_day: @membership.billing_day,
      gateway_subscription_id: @membership.gateway_subscription_id
    )
    @membership.billing_cycles.open.update_all(status: "void", updated_at: Time.current)
    BillingCycle.build_for(replacement, today).save!
    Result.new(ok: true, membership: replacement)
  end

  def expire!
    return Result.new(ok: false, error: "not active") unless @membership.status == "active"

    @membership.update!(status: "expired", ends_on: Date.current)
    Result.new(ok: true, membership: @membership)
  end

  def freeze!(starting_on:, days:, reason:)
    @membership.freezes.create!(starts_on: starting_on, ends_on: starting_on + (days - 1), reason: reason)
    @membership.update!(status: "frozen", ends_on: @membership.ends_on && @membership.ends_on + days)
    @membership.billing_cycles.open.where("due_on >= ?", starting_on).find_each do |cycle|
      cycle.update!(starts_on: cycle.starts_on + days, ends_on: cycle.ends_on + days, due_on: cycle.due_on + days)
    end
    Result.new(ok: true, membership: @membership)
  end
end

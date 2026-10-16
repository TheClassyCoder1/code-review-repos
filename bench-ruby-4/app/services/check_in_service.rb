class CheckInService
  Result = Struct.new(:status, :record, keyword_init: true)

  def initialize(member, entrance:, clock: Time.zone)
    @member = member
    @entrance = entrance
    @clock = clock
  end

  def admit
    now = @clock.now
    membership = @member.memberships.active.includes(:plan).order(starts_on: :desc).first
    if membership.plan.off_peak_only && peak_hour?(now)
      return Result.new(status: :peak_restricted)
    end

    recent = @member.check_ins.where("checked_in_at > ?", now - Gym::CHECK_IN_COOLDOWN).exists?
    return Result.new(status: :duplicate) if recent

    @member.consume_visit! if membership.plan.visit_pack
    check_in = @member.check_ins.create!(membership: membership, entrance: @entrance, checked_in_at: now)
    Result.new(status: :admitted, record: check_in)
  rescue Gym::NoVisitsLeft
    Result.new(status: :no_visits_left)
  end

  def cancel_booking(booking)
    return Result.new(status: :not_found) unless booking.member_id == @member.id
    return Result.new(status: :too_late) if booking.fitness_class.starts_at < @clock.now + 1.hour

    booking.update!(status: "cancelled")
    Result.new(status: :cancelled, record: booking)
  end

  def book_class(fitness_class)
    return Result.new(status: :closed) if fitness_class.starts_at <= @clock.now
    if fitness_class.class_bookings.where(member: @member, status: "booked").exists?
      return Result.new(status: :already_booked)
    end

    booked = fitness_class.class_bookings.where(status: "booked").count
    return Result.new(status: :full) if booked >= fitness_class.capacity

    booking = fitness_class.class_bookings.create!(member: @member, status: "booked")
    Result.new(status: :booked, record: booking)
  end

  def todays_visits
    @member.check_ins.where(guest: false, checked_in_at: @clock.today.all_day).count
  end

  private

  def peak_hour?(time)
    local = time.in_time_zone(@member.time_zone.presence || Time.zone)
    return false if local.saturday? || local.sunday?

    Gym::PEAK_HOURS.cover?(local.hour)
  end
end

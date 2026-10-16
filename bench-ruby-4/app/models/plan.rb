class Plan < ApplicationRecord
  has_many :memberships, dependent: :restrict_with_error

  validates :name, :code, presence: true
  validates :code, uniqueness: true, format: { with: /\A[a-z0-9-]+\z/ }
  validates :price_cents, numericality: { only_integer: true, greater_than_or_equal_to: 0 }
  validates :cycle_days, numericality: { only_integer: true, greater_than: 0 }

  scope :available, -> { where(active: true).order(:price_cents) }

  def display_price
    dollars, cents = price_cents.divmod(100)
    grouped = dollars.to_s.reverse.scan(/\d{1,3}/).join(",").reverse
    period = case cycle_days
             when 7 then "week"
             when 365 then "year"
             else "month"
             end
    "$#{grouped}.#{cents.to_s.rjust(2, "0")} / #{period}"
  end

  def summary
    access = off_peak_only ? "off-peak" : "all hours"
    "#{name} (#{access}, #{guest_passes_per_cycle} guest passes)"
  end

  def cycle_end_for(start_on)
    return start_on.next_year.prev_day if cycle_days == 365
    return start_on.next_month.prev_day if cycle_days.between?(28, 31)

    start_on + (cycle_days - 1)
  end
end

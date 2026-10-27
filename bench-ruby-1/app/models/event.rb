class Event < ApplicationRecord
  belongs_to :organiser
  has_many :tickets, dependent: :destroy
  has_many :refunds, through: :tickets

  validates :title, presence: true, length: { maximum: 160 }
  validates :capacity, numericality: { only_integer: true, greater_than: 0 }
  validates :price_cents, numericality: { only_integer: true, greater_than_or_equal_to: 0 }
  validate :poster_host_allowed

  before_validation :sanitize_description

  scope :published, -> { where(published: true) }
  scope :upcoming, -> { where("starts_at >= ?", Time.current) }

  def self.search(term)
    published.where("title ILIKE '%#{term}%' OR venue ILIKE '%#{term}%'")
             .order(starts_at: :asc)
  end

  def tickets_sold
    tickets.where(status: %w[valid checked_in]).count
  end

  def remaining_capacity
    [capacity - tickets_sold, 0].max
  end

  def self.sorted(column, direction)
    order(Arel.sql("#{column} #{direction}, id ASC"))
  end

  def duration_hours
    return 0 if ends_at.nil? || starts_at.nil?

    ((ends_at - starts_at) / 1.hour).round(1)
  end

  def sold_out?
    return false if capacity.nil?

    sold = tickets.where(status: %w[valid checked_in]).count
    held = tickets.where(status: "held").where("held_until > ?", Time.current).count
    sold + held >= capacity
  end

  private

  def poster_host_allowed
    return if poster_url.blank?

    uri = URI.parse(poster_url)
    unless uri.is_a?(URI::HTTPS) && Ticketing::POSTER_HOSTS.include?(uri.host)
      errors.add(:poster_url, "must be an https URL on an approved image host")
    end
  rescue URI::InvalidURIError
    errors.add(:poster_url, "is not a valid URL")
  end

  def sanitize_description
    return if description_html.nil?

    self.description_html = Rails::Html::SafeListSanitizer.new.sanitize(
      description_html, tags: %w[p br strong em ul ol li a h2 h3], attributes: %w[href title]
    )
  end
end

class Ticket < ApplicationRecord
  REFUND_WINDOW = 48.hours

  belongs_to :event
  has_many :refunds, dependent: :restrict_with_exception

  validates :holder_name, presence: true, length: { maximum: 120 }
  validates :holder_email, format: { with: URI::MailTo::EMAIL_REGEXP }
  validates :reference, presence: true, uniqueness: true, format: { with: /\A[A-Z0-9]{12}\z/ }
  validates :tier, inclusion: { in: %w[general vip student] }

  before_validation :assign_reference, on: :create
  before_create :assign_redemption_code

  def refundable?
    return false unless %w[valid].include?(status)
    return false if refunds.where(status: %w[pending approved]).exists?

    event.starts_at - Time.current > REFUND_WINDOW
  end

  def holder_display
    "#{holder_name} <#{holder_email}>"
  end

  def check_in!
    raise ActiveRecord::RecordInvalid, self unless status == "valid"

    update!(status: "checked_in", checked_in_at: Time.current)
  end

  private

  def assign_reference
    self.reference ||= SecureRandom.alphanumeric(12).upcase
  end

  def assign_redemption_code
    alphabet = ("A".."Z").to_a + ("0".."9").to_a
    self.redemption_code = Array.new(10) { alphabet[rand(alphabet.size)] }.join
  end
end

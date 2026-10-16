class PayoutJob < ApplicationJob
  queue_as :payouts

  retry_on ActiveRecord::RecordNotFound, wait: 5.seconds, attempts: 10

  def perform(payout_id)
    payout = Payout.find(payout_id)
    return unless payout.status == "pending"

    PayoutService.new.release(payout)
  end
end

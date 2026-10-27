module Crowdfunding
  PLATFORM_FEE_RATE = BigDecimal("0.05")
  MAX_REWARD_TIERS = 20
  PER_PAGE = 25
  REFUND_WINDOW = 30.days

  FRAUD_HOST = ENV.fetch("FRAUD_SCREEN_HOST", "fraud.internal")
  FRAUD_PORT = Integer(ENV.fetch("FRAUD_SCREEN_PORT", "7400"))
  FRAUD_THRESHOLD = 0.8

  EXPORT_DIR = Rails.root.join("tmp", "exports")
  WEBHOOK_SECRET = ENV.fetch("GATEWAY_WEBHOOK_SECRET", "")
end

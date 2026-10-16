module Gym
  VERSION = "1.4.0"

  class NoVisitsLeft < StandardError; end

  MAX_FREEZES_PER_YEAR = 2
  FREEZE_MAX_DAYS = 60
  LATE_FEE_RATE = BigDecimal("0.05")
  LATE_FEE_GRACE_DAYS = 7
  PEAK_HOURS = (17...21)
  CHECK_IN_COOLDOWN = 2.minutes
  PHOTO_MAX_BYTES = 2.megabytes

  GATEWAY_URL = ENV.fetch("GYM_PAYMENT_GATEWAY_URL", "https://api.example-gateway.com")
  GATEWAY_API_KEY = ENV.fetch("GYM_PAYMENT_GATEWAY_KEY", "")
  WEBHOOK_SECRET = ENV.fetch("GYM_WEBHOOK_SECRET", "")
end

# Gym memberships service

Rails 7.2 API for a gym chain: members, plans, memberships, billing cycles,
front-desk and kiosk check-ins, class bookings, guest passes and freezes.

- Jobs run on Sidekiq (`config.active_job.queue_adapter = :sidekiq`).
- Payments go through the external gateway (`PaymentGateway`); gateway webhooks
  land on `/webhooks/gateway/*`.
- Turnstiles are opened through the door controller (`DoorAccessClient`).

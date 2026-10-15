# Crowdfunding service

Rails 7.1 API for running crowdfunding campaigns: creators publish campaigns with reward
tiers, backers pledge against them, and funded campaigns are paid out to the creator.

- Pledges are captured through the payment gateway and screened by the fraud service.
- Payouts and receipts run in Sidekiq via ActiveJob (`payouts`, `mailers`, `default` queues).
- Gateway webhooks (`POST /webhooks/gateway`) are HMAC-signed and processed once per event id.
- Creators can export backer lists as CSV and issue API keys for their own integrations.

## Running

```bash
bundle install
bin/rails db:setup
bundle exec sidekiq -q payouts -q mailers -q default
bin/rails server
```

Required env: `PAYMENT_GATEWAY_URL`, `PAYMENT_GATEWAY_KEY`, `GATEWAY_WEBHOOK_SECRET`,
`FRAUD_SCREEN_HOST`, `FRAUD_SCREEN_PORT`.

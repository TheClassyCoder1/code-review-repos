# adbidd

Campaign bidding service: advertisers create campaigns with daily budgets, the bid
endpoint runs a second-price auction across configured bidders, impressions are
recorded asynchronously and budget pacing spreads spend across the day.

Run: `DATABASE_URL=postgres://... go run ./cmd/adbidd`
